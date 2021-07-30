package chapter4

import zio.*

object Chapter4Exs:

  /* ** ** 1 ** ** */

  // ZIO.succeed will convert thrown exceptions to defects
  def failWithMessageOrig(string: String): UIO[Nothing] =
    ZIO.succeed(throw new Error(string))

  // We can search for a defect using dieOption and then change it to a failure
  // (due to Cause being recursive
  // we can't directly inspect the outer Cause)
  def failWithMessageCaught(string: String): Task[Nothing] =
    failWithMessageOrig(string).sandbox.mapError { cause =>
      cause.dieOption match {
        case Some(thr) => Cause.Fail(thr)
        case None      => cause
      }
    }.unsandbox

  // The better solution is to use a different effect constructor
  def failWithMessage(string: String): Task[Nothing] =
    ZIO.attempt(throw new Error(string))
