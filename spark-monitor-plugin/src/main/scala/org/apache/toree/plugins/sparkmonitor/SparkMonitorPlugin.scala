/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.plugins.sparkmonitor

import org.apache.toree.plugins.{AllInterpretersReady, Plugin, PluginManager, SparkReady}
import org.apache.toree.plugins.annotations.{Event, Init}
import org.apache.toree.plugins.dependencies.Dependency
import org.apache.toree.kernel.api.KernelLike
import org.apache.toree.kernel.api.Kernel
import org.apache.toree.comm.{CommRegistrar, CommWriter}
import org.apache.toree.kernel.protocol.v5.{MsgData, UUID}
import org.apache.toree.utils.ScheduledTaskManager
import org.apache.spark.SparkContext
import org.apache.log4j.Logger

import scala.util.Try
import scala.reflect.runtime.universe
import java.lang.reflect.Field

/**
 * Plugin that registers a JupyterSparkMonitorListener to SparkContext after kernel started
 * and provides communication through a "SparkMonitor" comm target.
 * 
 * This plugin uses proper typing where possible and falls back to reflection only when necessary
 * to maintain compatibility across different kernel implementations.
 */
class SparkMonitorPlugin extends Plugin {

  private val logger = Logger.getLogger(this.getClass.getName)
  private var sparkMonitorListener: Option[JupyterSparkMonitorListener] = None
  private var commWriter: Option[CommWriter] = None
  private val taskManager = new ScheduledTaskManager()
  private var sparkContextMonitorTaskId: Option[String] = None
  private var currentKernel: Option[KernelLike] = None
  private var pluginManager: Option[PluginManager] = None
  
  // Communication target name - extracted as constant for better maintainability
  private val COMM_TARGET_NAME = "SparkMonitor"

  /**
   * Initialize the plugin by registering the SparkMonitor comm target.
   * Uses proper typing with safe casting and comprehensive error handling.
   * 
   * @param kernel The kernel instance implementing KernelLike interface
   */
  @Init
  def initializePlugin(kernel: KernelLike): Unit = {
    logger.info(s"Initializing SparkMonitor plugin with comm target: $COMM_TARGET_NAME")
    
    Try {
      // Cast to concrete Kernel type to access comm property. This plugin need to use toree-kernel module
      val concreteKernel = kernel.asInstanceOf[Kernel]
      val commManager = concreteKernel.comm
      
      // Register comm target - now we can use proper types
      val commRegistrar = commManager.register(COMM_TARGET_NAME)
      setupCommHandlers(commRegistrar)
      
      // Start background process to monitor SparkContext creation
      startSparkContextMonitoring()
      
      logger.info("SparkMonitor plugin initialized successfully")
    }.recover {
      case ex => logger.error(s"Failed to initialize SparkMonitor plugin: ${ex.getMessage}", ex)
    }
  }
  
  /**
   * Initialize the plugin manager reference.
   *
   * @param manager The plugin manager instance
   */
  @Init
  def initializePluginManager(manager: PluginManager): Unit = {
    logger.info("Initializing PluginManager reference")
    pluginManager = Some(manager)
  }
  
  /**
   * Sets up all communication handlers for the registered comm target.
   */
  private def setupCommHandlers(commRegistrar: CommRegistrar): Unit = {
    // Add open handler
    commRegistrar.addOpenHandler { (commWriter: CommWriter, commId: UUID, targetName: String, data: MsgData) =>
      Try {
        logger.info(s"SparkMonitor comm opened - ID: $commId, Target: $targetName")
        this.commWriter = Some(commWriter)
        
        // Send initial connection message
        val message = MsgData("msgtype" -> "commopen")
        commWriter.writeMsg(message)
      }.recover {
        case ex => logger.warn(s"Error in comm open handler: ${ex.getMessage}", ex)
      }
    }
    
    // Add message handler
    commRegistrar.addMsgHandler { (commWriter: CommWriter, commId: UUID, data: MsgData) =>
      logger.debug(s"SparkMonitor received message from comm $commId: $data")
      // Handle incoming messages from client if needed
      // This can be extended for bidirectional communication
    }
    
    // Add close handler
    commRegistrar.addCloseHandler { (commWriter: CommWriter, commId: UUID, data: MsgData) =>
      logger.info(s"SparkMonitor comm closed - ID: $commId")
      this.commWriter = None
    }
  }

  /**
   * Starts a background task to monitor SparkContext creation.
   * This task runs periodically to check if SparkContext becomes available using reflection.
   */
  private def startSparkContextMonitoring(): Unit = {
    logger.info("Starting SparkContext monitoring background task")
    
    val taskId = taskManager.addTask(
      executionDelay = 1000, // Start checking after 1 second
      timeInterval = 2000,    // Check every 2 seconds
      task = {
        try {
          logger.info("Task execution started - this should appear every 2 seconds")
          checkSparkContextAndNotify()
          logger.info("Task execution completed")
        } catch {
          case ex: Exception =>
            logger.error("Task execution failed", ex)
        }
      }
    )
    
    sparkContextMonitorTaskId = Some(taskId)
    logger.debug(s"SparkContext monitoring task started with ID: $taskId")
  }
  
  /**
   * Checks if SparkContext is available using reflection to access private activeContext field.
   * Once SparkContext is found, stops the monitoring task and fires SparkReady event.
   *
   * Uses reflection to safely access SparkContext.activeContext without triggering instantiation.
   */
  private def checkSparkContextAndNotify(): Unit = {
    Try {
      logger.debug("checkSparkContextAndNotify is running")
      getActiveSparkContext() match {
        case Some(sparkContext) if !sparkContext.isStopped =>
          logger.info("SparkContext detected! Firing SparkReady event.")
          
          // Stop the monitoring task since SparkContext is now available
          stopSparkContextMonitoring()
          
          // Fire SparkReady event through plugin manager to notify all plugins
          fireSparkReadyEvent()
          
        case Some(sparkContext) =>
          logger.debug("SparkContext exists but is stopped, continuing to monitor...")
          
        case None =>
          logger.debug("No SparkContext found, continuing to monitor...")
      }
    }.recover {
      case ex =>
        logger.debug(s"Error checking SparkContext availability: ${ex.getMessage}")
    }
  }
  
  /**
   * Uses reflection to safely access the private activeContext field from SparkContext.
   * This approach doesn't trigger SparkContext instantiation.
   */
  private def getActiveSparkContext(): Option[SparkContext] = {
    Try {
      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val moduleSymbol = runtimeMirror.staticModule("org.apache.spark.SparkContext")
      val moduleMirror = runtimeMirror.reflectModule(moduleSymbol)
      val sparkContext = moduleMirror.instance

      val activeContextField: Field = sparkContext.getClass().getDeclaredField("org$apache$spark$SparkContext$$activeContext")
      activeContextField.setAccessible(true)

      val activeContextRef = activeContextField.get(sparkContext).asInstanceOf[java.util.concurrent.atomic.AtomicReference[SparkContext]]
      Option(activeContextRef.get())
    }.recover {
      case ex =>
        logger.error(s"Failed to access activeContext field via reflection: ${ex.getMessage} \n $ex")
        None
    }.getOrElse(None)
  }
  
  /**
   * Fires a SparkReady event through the plugin manager to notify all listening plugins.
   */
  private def fireSparkReadyEvent(): Unit = {
    Try {
      // Create a KernelLike dependency to pass along with the event
      pluginManager match {
        case Some(manager) =>
          manager.fireEvent(SparkReady)
          logger.info("SparkReady event fired to all plugins")
        case _ =>
          logger.warn("Cannot fire SparkReady event: kernel or plugin manager not available")
      }
    }.recover {
      case ex => logger.warn(s"Failed to fire SparkReady event: ${ex.getMessage}")
    }
  }
  
  /**
   * Stops the SparkContext monitoring background task.
   */
  private def stopSparkContextMonitoring(): Unit = {
    sparkContextMonitorTaskId.foreach { taskId =>
      if (taskManager.removeTask(taskId)) {
        logger.info(s"SparkContext monitoring task stopped (ID: $taskId)")
      }
      sparkContextMonitorTaskId = None
    }
  }

  /**
   * Handle the SparkReady event to register the JupyterSparkMonitorListener.
   * This method is called when Spark becomes available in the kernel.
   * Uses direct access to SparkContext when possible.
   */
  @Event(name = "sparkReady")
  def onReady(kernel: KernelLike): Unit = {
    logger.info("SparkReady event received, registering JupyterSparkMonitorListener")
    
    // Stop monitoring task if still running since SparkContext is ready
    stopSparkContextMonitoring()
    
    Try {
      val sparkContext = kernel.sparkContext
      // Pass a callback function that always gets the current commWriter
      val listener = new JupyterSparkMonitorListener(() => commWriter)
      
      sparkContext.addSparkListener(listener)
      sparkMonitorListener = Some(listener)
      
      logger.info("JupyterSparkMonitorListener registered successfully")
      // notifySparkListenerRegistration()
    }.recover {
      case ex => logger.error(s"Failed to register JupyterSparkMonitorListener: ${ex.getMessage}", ex)
    }
  }
  
  /**
   * Notifies clients that the Spark listener has been registered.
   */
  private def notifySparkListenerRegistration(): Unit = {
    commWriter.foreach { writer =>
      Try {
        val message = MsgData("text/plain" -> "JupyterSparkMonitorListener registered")
        writer.writeMsg(message)
      }.recover {
        case ex => logger.warn(s"Failed to send SparkListener registration notification: ${ex.getMessage}")
      }
    }
  }
}