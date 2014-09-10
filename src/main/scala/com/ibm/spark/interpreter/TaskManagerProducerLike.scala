package com.ibm.spark.interpreter

import com.ibm.spark.utils.TaskManager

trait TaskManagerProducerLike {
  /**
   * Creates a new instance of TaskManager.
   *
   * @return The new TaskManager instance
   */
  def newTaskManager(): TaskManager
}

trait StandardTaskManagerProducer extends TaskManagerProducerLike {
  override def newTaskManager(): TaskManager = new TaskManager
}
