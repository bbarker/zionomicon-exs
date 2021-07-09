package chapter4

import zio.*

object Chapter4Exs:

  // 1
  def failWithMessageOrig(string: String) =
    ZIO.succeed(throw new Error(string))

  def failWithMessage(string: String) =
    ZIO.succeed(throw new Error(string)).sandbox