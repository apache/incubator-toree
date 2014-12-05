package com.ibm.spark.interpreter

/**
 * Represents interpreter results, mostly taken from the
 * tools.nsc.interpreter.Results object.
 */
object Results {
  abstract sealed class Result

  /** The line was interpreted successfully. */
  case object Success extends Result { override def toString =  "success" }

  /** The line was erroneous in some way. */
  case object Error extends Result { override def toString =  "error" }

    /** The input was incomplete.  The caller should request more input. */
  case object Incomplete extends Result { override def toString =  "incomplete"}

      /** The line was aborted before completed. */
  case object Aborted extends Result { override def toString =  "aborted" }
}

