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

package org.apache.toree.security

import java.security.Permission
import java.util.UUID

import scala.collection.immutable.HashMap

object KernelSecurityManager {
  val RestrictedGroupName = "restricted-" + UUID.randomUUID().toString

  /**
    * ThreadLocal to indicate that an exit from a restricted group thread is permitted.
    */
  private val tlEnableRestrictedExit:ThreadLocal[Boolean] = new ThreadLocal[Boolean]

  /**
   * Special case for this permission since the name changes with each status
   * code.
   */
  private val SystemExitPermissionName = "exitVM." // + status

  /**
   * Used to indicate which permissions to check. Only checks if the permission
   * is found in the keys and the value for that permission is true.
   */
  private val permissionsToCheck: Map[String, Boolean] = HashMap()

  /**
   * Checks whether the permission with the provided name is listed to be
   * checked.
   *
   * @param name The name of the permission
   *
   * @return True if the permission is listed to be checked, false otherwise
   */
  private def shouldCheckPermission(name: String): Boolean =
    permissionsToCheck.getOrElse(name, shouldCheckPermissionSpecialCases(name))

  /**
   * Checks whether the permission with the provided name is one of the special
   * cases that don't exist in the normal name conventions.
   *
   * @param name The name of the permission
   *
   * @return True if the permission is to be checked, false otherwise
   */
  private def shouldCheckPermissionSpecialCases(name: String): Boolean =
    name.startsWith(SystemExitPermissionName)

  /**
    * Sets tlEnableRestrictedExit to true if caller is in the restricted group.  This
    * method is intended to be called exclusively from the ShutdownHandler since that's
    * the only path by which we should permit System.exit to succeed within the notebook.
    * Note that dual SIGINTs occur from a non-restricted thread group and are also permitted.
    */
  def enableRestrictedExit() {
    val currentGroup = Thread.currentThread().getThreadGroup
    tlEnableRestrictedExit.set(currentGroup.getName == RestrictedGroupName)
  }
}

class KernelSecurityManager extends SecurityManager {
  import KernelSecurityManager._

  override def checkPermission(perm: Permission, context: scala.Any): Unit = {
    // TODO: Investigate why the StackOverflowError occurs in IntelliJ without
    //       this check for FilePermission related to this class
    // NOTE: The above problem does not happen when built with sbt pack
    if (perm.getActions == "read" &&
      perm.getName.contains(this.getClass.getSimpleName))
      return

    if (shouldCheckPermission(perm.getName))
      super.checkPermission(perm, context)
  }

  override def checkPermission(perm: Permission): Unit = {
    // TODO: Investigate why the StackOverflowError occurs in IntelliJ without
    //       this check for FilePermission related to this class
    // NOTE: The above problem does not happen when built with sbt pack
    if (perm.getActions == "read" &&
      perm.getName.contains(this.getClass.getSimpleName))
      return

    if (shouldCheckPermission(perm.getName))
      super.checkPermission(perm)
  }

  def _isRestrictedGroup(): Boolean = {
    // Returns true if this thread group is derived from the restricted group so as to
    // prevent System.exit(0) from sub-threads running in a different group.
    var isRestricted = false
    var currentGroup = Thread.currentThread().getThreadGroup

    while ( currentGroup != null && !isRestricted ) {
      if ( currentGroup.getName == RestrictedGroupName )
        isRestricted = true
      else
        currentGroup = currentGroup.getParent
    }
    isRestricted
  }

  override def checkExit(status: Int): Unit = {
    // Exit will be denied if this thread is derived from the restricted group AND the restricted
    // exit thread-local has not been enabled.  This will only happen when the request
    // came from jupyter via a (cell) execution_request indicating the cell is attempting
    // an exit call.  Proper notebook shutdown requests will go through the ShutdownHandler
    // where restricted exits are enabled and SIGINT (ctrl-c) requests are not performed
    // via the restricted group and thus allowed.
    //
    if ( _isRestrictedGroup() ) {
      val isRestrictedExitEnabled:Option[Boolean] = Some(tlEnableRestrictedExit.get())

      if ( !isRestrictedExitEnabled.getOrElse(false) ) {
        throw new SecurityException("Not allowed to invoke System.exit!")
      }
    }
    // Just in case this thread remains alive - force it to pass through ShutdownHandler
    tlEnableRestrictedExit.set(false)
  }
}
