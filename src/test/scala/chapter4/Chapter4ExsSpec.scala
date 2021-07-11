package chapter4

import chapter4.Chapter4Exs.*

import zio.*
import zio.test.Assertion.*
import zio.test.*

object Chapter4ExsSpec extends DefaultRunnableSpec:
  def spec = suite("Chapter 4 Exercises") {
    testM("failWithMessageOrig fails"){
      val errMsg = "ERROR!!!!111"
      for {
        foundDefect <- failWithMessageOrig(errMsg).sandbox.foldCauseM(
          cause => cause match {
            case Cause.Die(_) => UIO.succeed(true)
            case Cause.Traced(_,_) => UIO.succeed(true)
            case _ => UIO.succeed(false)
          },
          _ => UIO.succeed(false)
        )
      } yield assert(foundDefect)(isTrue)
    }
  }