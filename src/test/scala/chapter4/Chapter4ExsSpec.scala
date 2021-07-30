package chapter4

import chapter4.Chapter4Exs.*

import zio.*
import zio.test.Assertion.*
import zio.test.*

object Chapter4ExsSpec extends DefaultRunnableSpec:
  def spec = suite ("Chapter 4 Exercises") (
    test("failWithMessageOrig has defects"){
      val errMsg = "ERROR!!!!111"
      for {
        foundDefect <- failWithMessageOrig(errMsg).exit
      } yield assert(foundDefect)(dies(anything))
    },
    test("failWithMessageCaught has failures"){
      val errMsg = "ERROR!!!!222"
      for {
        foundFailure <- failWithMessageCaught(errMsg).exit
      } yield assert(foundFailure)(fails(anything))
    },
    test("failWithMessageCaught has failures"){
      val errMsg = "ERROR!!!!333"
      for {
        foundFailure <- failWithMessage(errMsg).exit
      } yield assert(foundFailure)(fails(anything))
    })