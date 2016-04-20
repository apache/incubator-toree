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

package org.apache.toree.comm

import org.apache.toree.annotations.Experimental
import org.apache.toree.kernel.protocol.v5

import scala.collection.immutable

/**
 * Represents the storage structure for Comm-related data.
 *
 * @param callbackStorage The structure used to connect targets with callbacks
 * @param linkStorage The structure used to connect targets to specific ids
 */
@Experimental
class CommStorage(
  private val callbackStorage: collection.mutable.Map[String, CommCallbacks] =
    new collection.mutable.HashMap[String, CommCallbacks](),
  private val linkStorage: collection.mutable.Map[String, immutable.IndexedSeq[v5.UUID]] =
    new collection.mutable.HashMap[String, immutable.IndexedSeq[v5.UUID]]()
) {
  /**
   * Sets the Comm callbacks for the specified target.
   *
   * @param targetName The name of the target whose callbacks to set
   * @param commCallbacks The new callbacks for the target
   */
  def setTargetCallbacks(targetName: String, commCallbacks: CommCallbacks) =
    callbackStorage(targetName) = commCallbacks

  /**
   * Removes the Comm callbacks from the specified target.
   *
   * @param targetName The name of the target whose callbacks to remove
   *
   * @return Some CommCallbacks if removed, otherwise None
   */
  def removeTargetCallbacks(targetName: String) =
    callbackStorage.remove(targetName)

  /**
   * Retrieves the current Comm callbacks for the specified target.
   *
   * @param targetName The name of the target whose callbacks to get
   *
   * @return Some CommCallbacks if found, otherwise None
   */
  def getTargetCallbacks(targetName: String): Option[CommCallbacks] =
    callbackStorage.get(targetName)

  /**
   * Determines if the specified target has any callbacks.
   *
   * @param targetName The name of the target
   *
   * @return True if a CommCallbacks instance is found, otherwise false
   */
  def hasTargetCallbacks(targetName: String) =
    callbackStorage.contains(targetName)

  /**
   * Sets the Comm ids associated with the specified target.
   *
   * @param targetName The name of the target whose Comm ids to set
   * @param links The sequence of Comm ids to attach to the target
   */
  def setTargetCommIds(
    targetName: String, links: immutable.IndexedSeq[v5.UUID]
  ) = linkStorage(targetName) = links

  /**
   * Removes the Comm ids associated with the specified target.
   *
   * @param targetName The name of the target whose Comm ids to remove
   *
   * @return Some sequence of Comm ids if removed, otherwise None
   */
  def removeTargetCommIds(targetName: String) = linkStorage.remove(targetName)

  /**
   * Removes the specified Comm id from the first target with a match.
   *
   * @param commId The Comm id to remove
   *
   * @return Some name of target linked to Comm id if removed, otherwise None
   */
  def removeCommIdFromTarget(commId: v5.UUID): Option[v5.UUID] = {
    val targetName = getTargetFromCommId(commId)

    targetName match {
      case Some(name) =>
        val commIds = getCommIdsFromTarget(name).get.filterNot(_ == commId)
        setTargetCommIds(name, commIds)
        Some(name)
      case None       =>
        None
    }
  }

  /**
   * Retrieves the current sequence of Comm ids for the specified target.
   *
   * @param targetName The name of the target whose Comm ids to get
   *
   * @return Some sequence of Comm ids if found, otherwise None
   */
  def getCommIdsFromTarget(targetName: String) = linkStorage.get(targetName)

  /**
   * Retrieves all registered target names
   *
   * @return Some set of target names
   */
  def getTargets() = linkStorage.keySet

  /**
   * Retrieves the current target for the specified Comm id.
   *
   * @param commId The Comm id whose target to get
   *
   * @return Some target name if found, otherwise None
   */
  def getTargetFromCommId(commId: v5.UUID): Option[String] = linkStorage.find {
    (link) => link._2.contains(commId)
  } match {
    case Some(link) => Some(link._1)
    case None       => None
  }

  /**
   * Retrieves the current Comm callbacks for the specified Comm id.
   *
   * @param commId The id of the Comm whose callbacks to retrieve
   *
   * @return Some CommCallbacks if Comm id found, otherwise None
   */
  def getCommIdCallbacks(commId: v5.UUID): Option[CommCallbacks] =
    getTargetFromCommId(commId) match {
      case Some(targetName) => getTargetCallbacks(targetName)
      case None             => None
    }

  /**
   * Determines if the specified target has any linked Comm ids.
   *
   * @param targetName The name of the target
   *
   * @return True if a sequence of Comm ids is found, otherwise false
   */
  def hasTargetCommIds(targetName: String) =
    linkStorage.contains(targetName)
}
