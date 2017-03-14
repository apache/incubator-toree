/**
 *  FIXME:  Correct License.  Borrowed from https://github.com/scalatest/scalatest/issues/491 
 *          
 */

package test.utils

import org.scalactic.Equality

trait CustomEquality {

  import scala.reflect.ClassTag

  /**
   * A custom [[Equality]] definition for [[Seq]] that understands how to compare [[Array]] elements in the set.
   *
   * To make use of this custom matcher for [[Seq]] you must `import CustomEquality._` and
   * then use `seq1 should equal(seq2)` instead of `seq1 should be(seq2)`.
   *
   * Note: Once ScalaTest 3.x is released this custom [[Equality]] definition will no longer be needed.
   * See https://github.com/scalatest/scalatest/issues/491 for details.
   */
  implicit def customEq[S: ClassTag]: Equality[Seq[S]] = new Equality[Seq[S]] {

    override def areEqual(left: Seq[S], right: Any): Boolean = {
      val equal = right match {
        case r: Seq[_] if implicitly[ClassTag[S]].runtimeClass.isArray =>
          val sameSize = left.size == r.size
          val sameContent = {
            val leftHashes: Seq[Int] = left.map {_.asInstanceOf[Array[_]].toSeq.hashCode()}
            val rightHashes: Seq[Int] = r.map {_.asInstanceOf[Array[_]].toSeq.hashCode()}
            leftHashes == rightHashes
          }
          sameSize && sameContent
        case _ => left == right
      }
      equal
    }

  }
}

// Make the custom equality definitions easy to import with: `import CustomEquality._`
object CustomEquality extends CustomEquality
