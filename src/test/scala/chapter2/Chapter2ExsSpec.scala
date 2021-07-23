package chapter2

import chapter2.Chapter2Exs.*

import zio.*
import zio.test.Assertion.*
import zio.test.*

object Chapter2ExsSpec extends DefaultRunnableSpec:
  def spec = suite("Chapter 2 Exercises") {
    test("todo"){
      assert(2+2)(equalTo(4))
    }

  }
