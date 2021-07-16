package chapter4

import chapter4.Chapter4Exs.*

import zio.*
import zio.test.Assertion.*
import zio.test.*

object Chapter4ExsSpec extends DefaultRunnableSpec:
  def spec = suite("Chapter 4 Exercises") {
    testM("failWithMessageOrig has defects"){
      val errMsg = "ERROR!!!!111"
      for {
        foundDefect <- failWithMessageOrig(errMsg).run
      } yield assert(foundDefect)(dies(anything))
    }
    testM("failWithMessageCaught has failures"){
      val errMsg = "ERROR!!!!222"
      for {
        foundFailure <- failWithMessageCaught(errMsg).run
      } yield assert(foundFailure)(fails(anything))
    }
    testM("failWithMessageCaught has failures"){
      val errMsg = "ERROR!!!!333"
      for {
        foundFailure <- failWithMessage(errMsg).run
      } yield assert(foundFailure)(fails(anything))
    }
  }