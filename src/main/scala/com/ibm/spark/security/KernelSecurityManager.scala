package com.ibm.spark.security

import java.security.Permission
import java.util.UUID

import scala.collection.immutable.HashMap

object KernelSecurityManager {
  val RestrictedGroupName = "restricted-" + UUID.randomUUID().toString

  /**
   * Special case for this permission since the name changes with each status
   * code.
   */
  private val SystemExitPermissionName = "exitVM." // + status

  /**
   * Used to indicate which permissions to check. Only checks if the permission
   * is found in the keys and the value for that permission is true.
   */
  private val permissionsToCheck: Map[String, Boolean] = HashMap(
    "modifyThreadGroup" -> true
  )

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

  override def getThreadGroup: ThreadGroup = {
    val currentGroup = Thread.currentThread().getThreadGroup

    // For restricted groups, we can only catch them in the checkAccess if we
    // set the current group as the parent (to make sure all groups have a
    // consistent name)
    if (currentGroup.getName == RestrictedGroupName) {
      new ThreadGroup(currentGroup, currentGroup.getName)
    } else {
      super.getThreadGroup
    }
  }

  override def checkAccess(g: ThreadGroup): Unit = {
    //super.checkAccess(g)
    if (g == null) return

    val parentGroup = g.getParent

    if (parentGroup != null &&
      parentGroup.getName == RestrictedGroupName &&
      g.getName != RestrictedGroupName)
      throw new SecurityException("Not allowed to modify ThreadGroups!")
  }

  override def checkExit(status: Int): Unit = {
    val currentGroup = Thread.currentThread().getThreadGroup

    if (currentGroup.getName == RestrictedGroupName)
      throw new SecurityException("Not allowed to invoke System.exit!")
  }
}
