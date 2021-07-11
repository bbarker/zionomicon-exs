package chapter4

import zio.*

object Chapter4Exs:

  // 1
  def failWithMessageOrig(string: String): UIO[Nothing] =
    ZIO.succeed(throw new Error(string))

  def failWithMessage(string: String): IO[Throwable, Nothing] =
    ZIO
      .effect(throw new Error(string))
      .sandbox
      .mapError {
        case Cause.Die(thr) => Cause.Fail(thr)
        case e              => e
      }
      .unsandbox
