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

import org.apache.spark.scheduler._
import play.api.libs.json._
import org.apache.spark._
import org.apache.spark.TaskEndReason
import org.apache.spark.JobExecutionStatus
import org.apache.spark.SparkContext
import org.apache.log4j.Logger
import scala.collection.mutable
import scala.collection.mutable.{ HashMap, HashSet, LinkedHashMap, ListBuffer }
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.util.{TimerTask,Timer}
import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.MsgData
import scala.util.Try

/**
 * A SparkListener Implementation that forwards data to a Jupyter Kernel via comm
 *
 *  - All data is forwarded to a jupyter kernel using comm channel.
 *  - The listener receives notifications of the spark application's events, through the overrided methods.
 *  - The received data is stored and sent as JSON to the kernel via comm.
 *  - Overrides methods that correspond to events in a spark Application.
 *  - The argument for each overrided method contains the received data for that event. (See SparkListener docs for more information.)
 *  - For each application, job, stage, and task there is a 'start' and an 'end' event. For executors, there are 'added' and 'removed' events
 *
 *  @constructor called by the plugin system
 *  @param commWriter The comm writer to send messages through
 */
class JupyterSparkMonitorListener(getCommWriter: () => Option[CommWriter]) extends SparkListener {

  val logger = Logger.getLogger(this.getClass.getName)
  logger.info("Started JupyterSparkMonitorListener for Jupyter Notebook")
  
  var onStageStatusActiveTask: TimerTask = null
  val sparkTasksQueue: BlockingQueue[String] = new LinkedBlockingQueue[String]()
  val sparkStageActiveTasksMaxMessages: Integer = 250
  val sparkStageActiveRate: Long = 1000L // 1s

  logger.info("Starting timer task for active stage monitoring")
  startActiveStageMonitoring()

  /** Send a JSON message via comm channel. */
  def send(json: JsValue): Unit = {
    val jsonString = Json.stringify(json)
    getCommWriter().foreach { writer =>
      try {
        // Create MsgData with the JSON content
        val msgData = MsgData("msgtype" -> "fromscala", "msg" -> jsonString)

        // Send message directly using CommWriter
        writer.writeMsg(msgData)

      } catch {
        case exception: Throwable =>
          logger.error("Exception sending comm message: ", exception)
          // Fallback: just log the message
          logger.debug(s"SparkMonitor event: $jsonString")
      }
    }

    // If no comm writer, just log the message
    if (getCommWriter().isEmpty) {
      logger.debug(s"SparkMonitor event (no comm): $jsonString")
    }
  }

  /** Start the active stage monitoring task. */
  def startActiveStageMonitoring(): Unit = {
    try {
      val t = new Timer()

      if (onStageStatusActiveTask == null) {
        onStageStatusActiveTask = new TimerTask {
          def run() = {
            onStageStatusActive()
          }
        }
      }
      t.schedule(onStageStatusActiveTask, sparkStageActiveRate, sparkStageActiveRate)
    } catch {
      case exception: Throwable => logger.error("Exception creating timer task: ", exception)
    }
  }

  /** Stop the active stage monitoring task. */
  def stopActiveStageMonitoring(): Unit = {
    logger.info("Stopping active stage monitoring")
    if (onStageStatusActiveTask != null) {
      onStageStatusActiveTask.cancel()
    }
  }

  type JobId = Int
  type JobGroupId = String
  type StageId = Int
  type StageAttemptId = Int

  //Application
  @volatile var startTime = -1L
  @volatile var endTime = -1L
  var appId: String = ""

  //Jobs
  val activeJobs = new HashMap[JobId, UIData.JobUIData]
  val completedJobs = ListBuffer[UIData.JobUIData]()
  val failedJobs = ListBuffer[UIData.JobUIData]()
  val jobIdToData = new HashMap[JobId, UIData.JobUIData]
  val jobGroupToJobIds = new HashMap[JobGroupId, HashSet[JobId]]

  // Stages:
  val pendingStages = new HashMap[StageId, StageInfo]
  val activeStages = new HashMap[StageId, StageInfo]
  val completedStages = ListBuffer[StageInfo]()
  val skippedStages = ListBuffer[StageInfo]()
  val failedStages = ListBuffer[StageInfo]()
  val stageIdToData = new HashMap[(StageId, StageAttemptId), UIData.StageUIData]
  val stageIdToInfo = new HashMap[StageId, StageInfo]
  val stageIdToActiveJobIds = new HashMap[StageId, HashSet[JobId]]

  var numCompletedStages = 0
  var numFailedStages = 0
  var numCompletedJobs = 0
  var numFailedJobs = 0

  val retainedStages = 1000
  val retainedJobs = 1000
  val retainedTasks = 100000

  @volatile
  var totalNumActiveTasks = 0
  val executorCores = new HashMap[String, Int]
  @volatile var totalCores: Int = 0
  @volatile var numExecutors: Int = 0

  /**
   * Called when a spark application starts.
   *
   * The application start time and app ID are obtained here.
   */
  override def onApplicationStart(appStarted: SparkListenerApplicationStart): Unit = {
    startTime = appStarted.time
    appId = appStarted.appId.getOrElse("null")
    logger.info("Application Started: " + appId + "  ...Start Time: " + appStarted.time)
    val json = Json.obj(
      "msgtype" -> "sparkApplicationStart",
      "startTime" -> startTime,
      "appId" -> appId,
      "appAttemptId" -> appStarted.appAttemptId.getOrElse[String]("null"),
      "appName" -> appStarted.appName,
      "sparkUser" -> appStarted.sparkUser
    )

    send(json)
  }

  /**
   * Called when a spark application ends.
   *
   * Stops the active stage monitoring task.
   */
  override def onApplicationEnd(appEnded: SparkListenerApplicationEnd): Unit = {
    logger.info("Application ending...End Time: " + appEnded.time)
    endTime = appEnded.time
    val json = Json.obj(
      "msgtype" -> "sparkApplicationEnd",
      "endTime" -> endTime
    )

    send(json)
    stopActiveStageMonitoring()
  }

  /** Converts stageInfo object to a JSON object. */
  def stageInfoToJSON(stageInfo: StageInfo): JsObject = {
    val completionTime: Long = stageInfo.completionTime.getOrElse(-1)
    val submissionTime: Long = stageInfo.submissionTime.getOrElse(-1)

    Json.obj(
      stageInfo.stageId.toString -> Json.obj(
        "attemptId" -> stageInfo.attemptNumber(),
        "name" -> stageInfo.name,
        "numTasks" -> stageInfo.numTasks,
        "completionTime" -> completionTime,
        "submissionTime" -> submissionTime
      )
    )
  }

  /**
   * Called when a job starts.
   *
   * The jobStart object contains the list of planned stages. They are stored for tracking skipped stages.
   * The total number of tasks is also estimated from the list of planned stages,
   */
  override def onJobStart(jobStart: SparkListenerJobStart): Unit = synchronized {

    val jobGroup = for (
      props <- Option(jobStart.properties);
      group <- Option(props.getProperty("spark.jobGroup.id"))
    ) yield group

    val jobData: UIData.JobUIData =
      new UIData.JobUIData(
        jobId = jobStart.jobId,
        submissionTime = Option(jobStart.time).filter(_ >= 0),
        stageIds = jobStart.stageIds,
        jobGroup = jobGroup,
        status = JobExecutionStatus.RUNNING)
    jobGroupToJobIds.getOrElseUpdate(jobGroup.orNull, new HashSet[JobId]).add(jobStart.jobId)
    jobStart.stageInfos.foreach(x => pendingStages(x.stageId) = x)

    // Merge all stage info objects into one JSON object
    val stageinfojson = jobStart.stageInfos.foldLeft(Json.obj()) { (acc, stageInfo) =>
      acc ++ stageInfoToJSON(stageInfo)
    }

    jobData.numTasks = {
      val allStages = jobStart.stageInfos
      val missingStages = allStages.filter(_.completionTime.isEmpty)
      missingStages.map(_.numTasks).sum
    }
    jobIdToData(jobStart.jobId) = jobData
    activeJobs(jobStart.jobId) = jobData
    for (stageId <- jobStart.stageIds) {
      stageIdToActiveJobIds.getOrElseUpdate(stageId, new HashSet[StageId]).add(jobStart.jobId)
    }
    // If there's no information for a stage, store the StageInfo received from the scheduler
    // so that we can display stage descriptions for pending stages:
    for (stageInfo <- jobStart.stageInfos) {
      stageIdToInfo.getOrElseUpdate(stageInfo.stageId, stageInfo)
      stageIdToData.getOrElseUpdate((stageInfo.stageId, stageInfo.attemptNumber()), new UIData.StageUIData)
    }
    val name = jobStart.properties.getProperty("callSite.short", "null")
    val json = Json.obj(
      "msgtype" -> "sparkJobStart",
      "jobGroup" -> jobGroup.getOrElse[String]("null"),
      "jobId" -> jobStart.jobId,
      "status" -> "RUNNING",
      "submissionTime" -> Option(jobStart.time).filter(_ >= 0),
      "stageIds" -> jobStart.stageIds,
      "stageInfos" -> stageinfojson,
      "numTasks" -> jobData.numTasks,
      "totalCores" -> totalCores,
      "appId" -> appId,
      "numExecutors" -> numExecutors,
      "name" -> name
    )
    logger.info("Job Start: " + jobStart.jobId)
    logger.debug(Json.prettyPrint(json))
    send(json)
  }

  /** Called when a job ends. */
  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = synchronized {
    val jobData = activeJobs.remove(jobEnd.jobId).getOrElse {
      logger.info("Job completed for unknown job: " + jobEnd.jobId)
      new UIData.JobUIData(jobId = jobEnd.jobId)
    }
    jobData.completionTime = Option(jobEnd.time).filter(_ >= 0)
    var status = "null"
    jobData.stageIds.foreach(pendingStages.remove)
    jobEnd.jobResult match {
      case JobSucceeded =>
        completedJobs += jobData
        trimJobsIfNecessary(completedJobs)
        jobData.status = JobExecutionStatus.SUCCEEDED
        status = "COMPLETED"
        numCompletedJobs += 1
      case _ =>
        failedJobs += jobData
        trimJobsIfNecessary(failedJobs)
        jobData.status = JobExecutionStatus.FAILED
        numFailedJobs += 1
        status = "FAILED"
    }
    for (stageId <- jobData.stageIds) {
      stageIdToActiveJobIds.get(stageId).foreach { jobsUsingStage =>
        jobsUsingStage.remove(jobEnd.jobId)
        if (jobsUsingStage.isEmpty) {
          stageIdToActiveJobIds.remove(stageId)
        }
        stageIdToInfo.get(stageId).foreach { stageInfo =>
          if (stageInfo.submissionTime.isEmpty) {
            // if this stage is pending, it won't complete, so mark it as "skipped":
            skippedStages += stageInfo
            trimStagesIfNecessary(skippedStages)
            jobData.numSkippedStages += 1
            jobData.numSkippedTasks += stageInfo.numTasks
          }
        }
      }
    }

    val json = Json.obj(
      "msgtype" -> "sparkJobEnd",
      "jobId" -> jobEnd.jobId,
      "status" -> status,
      "completionTime" -> jobData.completionTime
    )

    logger.info("Job End: " + jobEnd.jobId)
    logger.debug(Json.prettyPrint(json))

    send(json)
  }

  /** Called when a stage is completed. */
  override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = synchronized {
    val stage = stageCompleted.stageInfo
    stageIdToInfo(stage.stageId) = stage
    val stageData = stageIdToData.getOrElseUpdate((stage.stageId, stage.attemptNumber()), {
      logger.info("Stage completed for unknown stage " + stage.stageId)
      new UIData.StageUIData
    })
    var status = "UNKNOWN"
    activeStages.remove(stage.stageId)
    if (stage.failureReason.isEmpty) {
      completedStages += stage
      numCompletedStages += 1
      trimStagesIfNecessary(completedStages)
      status = "COMPLETED"
    } else {
      failedStages += stage
      numFailedStages += 1
      trimStagesIfNecessary(failedStages)
      status = "FAILED"
    }

    val jobIds = stageIdToActiveJobIds.get(stage.stageId)
    for (
      activeJobsDependentOnStage <- jobIds;
      jobId <- activeJobsDependentOnStage;
      jobData <- jobIdToData.get(jobId)
    ) {
      jobData.numActiveStages -= 1
      if (stage.failureReason.isEmpty) {
        if (stage.submissionTime.isDefined) {
          jobData.completedStageIndices.add(stage.stageId)
        }
      } else {
        jobData.numFailedStages += 1
      }
    }
    val completionTime: Long = stage.completionTime.getOrElse(-1)
    val submissionTime: Long = stage.submissionTime.getOrElse(-1)
    val json = Json.obj(
      "msgtype" -> "sparkStageCompleted",
      "stageId" -> stage.stageId,
      "stageAttemptId" -> stage.attemptNumber(),
      "completionTime" -> completionTime,
      "submissionTime" -> submissionTime,
      "numTasks" -> stage.numTasks,
      "numFailedTasks" -> stageData.numFailedTasks,
      "numCompletedTasks" -> stageData.numCompletedTasks,
      "status" -> status,
      "jobIds" -> jobIds
    )

    logger.info("Stage Completed: " + stage.stageId)
    logger.debug(Json.prettyPrint(json))
    send(json)
  }

  /** Called when a stage is submitted for execution. */
  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit = synchronized {
    val stage = stageSubmitted.stageInfo
    activeStages(stage.stageId) = stage
    pendingStages.remove(stage.stageId)
    stageIdToInfo(stage.stageId) = stage
    val stageData = stageIdToData.getOrElseUpdate((stage.stageId, stage.attemptNumber()), new UIData.StageUIData)
    stageData.description = Option(stageSubmitted.properties).flatMap {
      p => Option(p.getProperty("spark.job.description"))
    }

    for (
      activeJobsDependentOnStage <- stageIdToActiveJobIds.get(stage.stageId);
      jobId <- activeJobsDependentOnStage;
      jobData <- jobIdToData.get(jobId)
    ) {
      jobData.numActiveStages += 1
      // If a stage retries again, it should be removed from completedStageIndices set
      jobData.completedStageIndices.remove(stage.stageId)
    }
    val activeJobsDependentOnStage = stageIdToActiveJobIds.get(stage.stageId)
    val jobIds = activeJobsDependentOnStage
    val submissionTime: Long = stage.submissionTime.getOrElse(-1)
    val json = Json.obj(
      "msgtype" -> "sparkStageSubmitted",
      "stageId" -> stage.stageId,
      "stageAttemptId" -> stage.attemptNumber(),
      "name" -> stage.name,
      "numTasks" -> stage.numTasks,
      "parentIds" -> stage.parentIds,
      "submissionTime" -> submissionTime,
      "jobIds" -> jobIds
    )
    logger.info("Stage Submitted: " + stage.stageId)
    logger.debug(Json.prettyPrint(json))
    send(json)
  }

  /** Called when scheduled stage tasks update was requested */
  def onStageStatusActive(): Unit = {
    // Update on status of active stages
    for ((stageId, stageInfo) <- activeStages) {
      val stageData = stageIdToData.getOrElseUpdate((stageInfo.stageId, stageInfo.attemptNumber()), new UIData.StageUIData)
      val jobIds = stageIdToActiveJobIds.get(stageInfo.stageId)

      val json = Json.obj(
        "msgtype" -> "sparkStageActive",
        "stageId" -> stageInfo.stageId,
        "stageAttemptId" -> stageInfo.attemptNumber(),
        "name" -> stageInfo.name,
        "parentIds" -> stageInfo.parentIds,
        "numTasks" -> stageInfo.numTasks,
        "numActiveTasks" -> stageData.numActiveTasks,
        "numFailedTasks" -> stageData.numFailedTasks,
        "numCompletedTasks" -> stageData.numCompletedTasks,
        "jobIds" -> jobIds
      )

      logger.info("Stage Update: " + stageInfo.stageId)
      logger.debug(Json.prettyPrint(json))
      send(json)
    }

    // Emit sparkStageActiveTasksMaxMessages spark tasks details from queue to frontend
    var count: Integer = 0
    while (sparkTasksQueue != null && !sparkTasksQueue.isEmpty() && count <= sparkStageActiveTasksMaxMessages) {
      count = count + 1
      val jsonString = sparkTasksQueue.take()
      // Already stringified JSON, parse and send
      Try(Json.parse(jsonString)).foreach(send)
    }

    if (count > 0) {
      logger.info("Stage Tasks details updated: " + count)
    }
  }

  /** Called when a task is started. */
  override def onTaskStart(taskStart: SparkListenerTaskStart): Unit = synchronized {
    val taskInfo = taskStart.taskInfo
    if (taskInfo != null) {
      val stageData = stageIdToData.getOrElseUpdate((taskStart.stageId, taskStart.stageAttemptId), {
        logger.info("Task start for unknown stage " + taskStart.stageId)
        new UIData.StageUIData
      })
      stageData.numActiveTasks += 1
    }
    var jobjson = Json.obj("jobdata" -> "taskstart")
    for (
      activeJobsDependentOnStage <- stageIdToActiveJobIds.get(taskStart.stageId);
      jobId <- activeJobsDependentOnStage;
      jobData <- jobIdToData.get(jobId)
    ) {
      jobData.numActiveTasks += 1
      val jobjson = Json.obj(
        "jobdata" -> Json.obj(
          "jobId" -> jobData.jobId,
          "numTasks" -> jobData.numTasks,
          "numActiveTasks" -> jobData.numActiveTasks,
          "numCompletedTasks" -> jobData.numCompletedTasks,
          "numSkippedTasks" -> jobData.numSkippedTasks,
          "numFailedTasks" -> jobData.numFailedTasks,
          "reasonToNumKilled" -> jobData.reasonToNumKilled,
          "numActiveStages" -> jobData.numActiveStages,
          "numSkippedStages" -> jobData.numSkippedStages,
          "numFailedStages" -> jobData.numFailedStages
        )
      )
    }
    val json = Json.obj(
      "msgtype" -> "sparkTaskStart",
      "launchTime" -> taskInfo.launchTime,
      "taskId" -> taskInfo.taskId,
      "stageId" -> taskStart.stageId,
      "stageAttemptId" -> taskStart.stageAttemptId,
      "index" -> taskInfo.index,
      "attemptNumber" -> taskInfo.attemptNumber,
      "executorId" -> taskInfo.executorId,
      "host" -> taskInfo.host,
      "status" -> taskInfo.status,
      "speculative" -> taskInfo.speculative
    )

    logger.info("Task Start: " + taskInfo.taskId)
    logger.debug(Json.prettyPrint(json))

    // Buffer the message for periodic flushing
    sparkTasksQueue.put(Json.stringify(json))
  }

  /** Called when a task is ended. */
  override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = synchronized {
    val info = taskEnd.taskInfo
    // If stage attempt id is -1, it means the DAGScheduler had no idea which attempt this task
    // completion event is for. Let's just drop it here. This means we might have some speculation
    // tasks on the web ui that's never marked as complete.
    var errorMessage: Option[String] = None
    if (info != null && taskEnd.stageAttemptId != -1) {
      val stageData = stageIdToData.getOrElseUpdate((taskEnd.stageId, taskEnd.stageAttemptId), {
        logger.info("Task end for unknown stage " + taskEnd.stageId)
        new UIData.StageUIData
      })
      stageData.numActiveTasks -= 1
      errorMessage = taskEnd.reason match {
        case org.apache.spark.Success =>
          stageData.completedIndices.add(info.index)
          stageData.numCompletedTasks += 1
          None
        case e: ExceptionFailure => // Handle ExceptionFailure because we might have accumUpdates
          stageData.numFailedTasks += 1
          Some(e.toErrorString)
        case e: TaskFailedReason => // All other failure cases
          stageData.numFailedTasks += 1
          Some(e.toErrorString)
      }

      for (
        activeJobsDependentOnStage <- stageIdToActiveJobIds.get(taskEnd.stageId);
        jobId <- activeJobsDependentOnStage;
        jobData <- jobIdToData.get(jobId)
      ) {
        jobData.numActiveTasks -= 1
        taskEnd.reason match {
          case Success =>
            jobData.numCompletedTasks += 1
          case _ =>
            jobData.numFailedTasks += 1
        }
      }
    }

    val totalExecutionTime = info.finishTime - info.launchTime
    def toProportion(time: Long) = time.toDouble / totalExecutionTime * 100
    var metricsOpt = Option(taskEnd.taskMetrics)
    val shuffleReadTime = metricsOpt.map(_.shuffleReadMetrics.fetchWaitTime).getOrElse(0L)
    val shuffleReadTimeProportion = toProportion(shuffleReadTime)
    val shuffleWriteTime = (metricsOpt.map(_.shuffleWriteMetrics.writeTime).getOrElse(0L) / 1e6).toLong
    val shuffleWriteTimeProportion = toProportion(shuffleWriteTime)
    val serializationTime = metricsOpt.map(_.resultSerializationTime).getOrElse(0L)
    val serializationTimeProportion = toProportion(serializationTime)
    val deserializationTime = metricsOpt.map(_.executorDeserializeTime).getOrElse(0L)
    val deserializationTimeProportion = toProportion(deserializationTime)
    val gettingResultTime = if (info.gettingResult) {
      if (info.finished) {
        info.finishTime - info.gettingResultTime
      } else {
        0L //currentTime - info.gettingResultTime
      }
    } else {
      0L
    }
    val gettingResultTimeProportion = toProportion(gettingResultTime)
    val executorOverhead = serializationTime + deserializationTime
    val executorRunTime = metricsOpt.map(_.executorRunTime).getOrElse(totalExecutionTime - executorOverhead - gettingResultTime)
    val schedulerDelay = math.max(0, totalExecutionTime - executorRunTime - executorOverhead - gettingResultTime)
    val schedulerDelayProportion = toProportion(schedulerDelay)
    val executorComputingTime = executorRunTime - shuffleReadTime - shuffleWriteTime
    val executorComputingTimeProportion =
      math.max(100 - schedulerDelayProportion - shuffleReadTimeProportion -
        shuffleWriteTimeProportion - serializationTimeProportion -
        deserializationTimeProportion - gettingResultTimeProportion, 0)

    val schedulerDelayProportionPos = 0
    val deserializationTimeProportionPos = schedulerDelayProportionPos + schedulerDelayProportion
    val shuffleReadTimeProportionPos = deserializationTimeProportionPos + deserializationTimeProportion
    val executorRuntimeProportionPos = shuffleReadTimeProportionPos + shuffleReadTimeProportion
    val shuffleWriteTimeProportionPos = executorRuntimeProportionPos + executorComputingTimeProportion
    val serializationTimeProportionPos = shuffleWriteTimeProportionPos + shuffleWriteTimeProportion
    val gettingResultTimeProportionPos = serializationTimeProportionPos + serializationTimeProportion

    val jsonMetrics = if (metricsOpt.isDefined) {
      Json.obj(
        "shuffleReadTime" -> shuffleReadTime,
        "shuffleWriteTime" -> shuffleWriteTime,
        "serializationTime" -> serializationTime,
        "deserializationTime" -> deserializationTime,
        "gettingResultTime" -> gettingResultTime,
        "executorComputingTime" -> executorComputingTime,
        "schedulerDelay" -> schedulerDelay,
        "shuffleReadTimeProportion" -> shuffleReadTimeProportion,
        "shuffleWriteTimeProportion" -> shuffleWriteTimeProportion,
        "serializationTimeProportion" -> serializationTimeProportion,
        "deserializationTimeProportion" -> deserializationTimeProportion,
        "gettingResultTimeProportion" -> gettingResultTimeProportion,
        "executorComputingTimeProportion" -> executorComputingTimeProportion,
        "schedulerDelayProportion" -> schedulerDelayProportion,
        "shuffleReadTimeProportionPos" -> shuffleReadTimeProportionPos,
        "shuffleWriteTimeProportionPos" -> shuffleWriteTimeProportionPos,
        "serializationTimeProportionPos" -> serializationTimeProportionPos,
        "deserializationTimeProportionPos" -> deserializationTimeProportionPos,
        "gettingResultTimeProportionPos" -> gettingResultTimeProportionPos,
        "executorComputingTimeProportionPos" -> executorRuntimeProportionPos,
        "schedulerDelayProportionPos" -> schedulerDelayProportionPos,
        "resultSize" -> metricsOpt.get.resultSize,
        "jvmGCTime" -> metricsOpt.get.jvmGCTime,
        "memoryBytesSpilled" -> metricsOpt.get.memoryBytesSpilled,
        "diskBytesSpilled" -> metricsOpt.get.diskBytesSpilled,
        "peakExecutionMemory" -> metricsOpt.get.peakExecutionMemory,
        "test" -> info.gettingResultTime
      )
    } else {
      Json.obj()
    }

    val json = Json.obj(
      "msgtype" -> "sparkTaskEnd",
      "launchTime" -> info.launchTime,
      "finishTime" -> info.finishTime,
      "taskId" -> info.taskId,
      "stageId" -> taskEnd.stageId,
      "taskType" -> taskEnd.taskType,
      "stageAttemptId" -> taskEnd.stageAttemptId,
      "index" -> info.index,
      "attemptNumber" -> info.attemptNumber,
      "executorId" -> info.executorId,
      "host" -> info.host,
      "status" -> info.status,
      "speculative" -> info.speculative,
      "errorMessage" -> errorMessage,
      "metrics" -> jsonMetrics
    )

    logger.info("Task Ended: " + info.taskId)
    logger.debug(Json.prettyPrint(json))

    // Buffer the message for periodic flushing
    sparkTasksQueue.put(Json.stringify(json))
  }

  /** If stored stages data is too large, remove and garbage collect old stages */
  private def trimStagesIfNecessary(stages: ListBuffer[StageInfo]) = synchronized {
    if (stages.size > retainedStages) {
      val toRemove = calculateNumberToRemove(stages.size, retainedStages)
      stages.take(toRemove).foreach { s =>
        stageIdToData.remove((s.stageId, s.attemptNumber()))
        stageIdToInfo.remove(s.stageId)
      }
      stages.trimStart(toRemove)
    }
  }

  /** If stored jobs data is too large, remove and garbage collect old jobs */
  private def trimJobsIfNecessary(jobs: ListBuffer[UIData.JobUIData]) = synchronized {
    if (jobs.size > retainedJobs) {
      val toRemove = calculateNumberToRemove(jobs.size, retainedJobs)
      jobs.take(toRemove).foreach { job =>
        // Remove the job's UI data, if it exists
        jobIdToData.remove(job.jobId).foreach { removedJob =>
          // A null jobGroupId is used for jobs that are run without a job group
          val jobGroupId = removedJob.jobGroup.orNull
          // Remove the job group -> job mapping entry, if it exists
          jobGroupToJobIds.get(jobGroupId).foreach { jobsInGroup =>
            jobsInGroup.remove(job.jobId)
            // If this was the last job in this job group, remove the map entry for the job group
            if (jobsInGroup.isEmpty) {
              jobGroupToJobIds.remove(jobGroupId)
            }
          }
        }
      }
      jobs.trimStart(toRemove)
    }
  }

  /** Calculate number of items to remove from stored data. */
  private def calculateNumberToRemove(dataSize: Int, retainedSize: Int): Int = {
    math.max(retainedSize / 10, dataSize - retainedSize)
  }

  /** Called when an executor is added. */
  override def onExecutorAdded(executorAdded: SparkListenerExecutorAdded): Unit = synchronized {
    executorCores(executorAdded.executorId) = executorAdded.executorInfo.totalCores
    totalCores += executorAdded.executorInfo.totalCores
    numExecutors += 1
    val json = Json.obj(
      "msgtype" -> "sparkExecutorAdded",
      "executorId" -> executorAdded.executorId,
      "time" -> executorAdded.time,
      "host" -> executorAdded.executorInfo.executorHost,
      "numCores" -> executorAdded.executorInfo.totalCores,
      "totalCores" -> totalCores // Sending this as browser data can be lost during reloads
    )

    logger.info("Executor Added: " + executorAdded.executorId)
    logger.debug(Json.prettyPrint(json))
    send(json)
  }

  /** Called when an executor is removed. */
  override def onExecutorRemoved(executorRemoved: SparkListenerExecutorRemoved): Unit = synchronized {
    totalCores -= executorCores.getOrElse(executorRemoved.executorId, 0)
    numExecutors -= 1
    val json = Json.obj(
      "msgtype" -> "sparkExecutorRemoved",
      "executorId" -> executorRemoved.executorId,
      "time" -> executorRemoved.time,
      "totalCores" -> totalCores // Sending this as browser data can be lost during reloads
    )

    logger.info("Executor Removed: " + executorRemoved.executorId)
    logger.debug(Json.prettyPrint(json))

    send(json)
  }
}

/** Data Structures for storing received from listener events. */
object UIData {

  /**
   * Data about a job.
   *
   * This is stored to track aggregated valus such as number of stages and tasks, and to track skipped and failed stages
   */
  class JobUIData(
    var jobId: Int = -1,
    var submissionTime: Option[Long] = None,
    var completionTime: Option[Long] = None,
    var stageIds: Seq[Int] = Seq.empty,
    var jobGroup: Option[String] = None,
    var status: JobExecutionStatus = JobExecutionStatus.UNKNOWN,
    var numTasks: Int = 0,
    var numActiveTasks: Int = 0,
    var numCompletedTasks: Int = 0,
    var numSkippedTasks: Int = 0,
    var numFailedTasks: Int = 0,
    var reasonToNumKilled: Map[String, Int] = Map.empty,
    var numActiveStages: Int = 0,
    // This needs to be a set instead of a simple count to prevent double-counting of rerun stages:
    var completedStageIndices: mutable.HashSet[Int] = new mutable.HashSet[Int](),
    var numSkippedStages: Int = 0,
    var numFailedStages: Int = 0)

  /**
   * Data about a stage.
   *
   * This is stored to track aggregated valus such as number of tasks.
   */
  class StageUIData {
    var numActiveTasks: Int = _
    var numCompletedTasks: Int = _
    var completedIndices = new HashSet[Int]()
    var numFailedTasks: Int = _
    var description: Option[String] = None
  }

  /**
   * Data about an executor.
   *
   * When an executor is removed, its number of cores is not available, so it is looked up here.
   */
  class ExecutorData {
    var numCores: Int = _
    var executorId: String = _
    var timeAdded: Long = _
    var timeRemoved: Long = _
    var executorHost: String = _
  }
}
