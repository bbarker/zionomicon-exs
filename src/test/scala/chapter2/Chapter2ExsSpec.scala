package chapter2

import chapter2.Chapter2Exs.*

import zio.*
import zio.test.Assertion.*
import zio.test.*
import java.io.IOException

object Chapter2ExsSpec extends DefaultRunnableSpec:
  val goodFile: String    = getClass.getResource("/goodFile.txt").getPath
  val missingFile: String = goodFile.replaceFirst("goodFile.txt", "missingFile.txt")

  val aaa111 = "aaa\n111"

  def spec = suite("Chapter 2 Exercises")(
    testReadFile("readFileZioNaive", readFileZioNaive),
    testReadFile("readFileZio", readFileZio),
    testReadFile("readFileZManaged", readFileZManaged.andThen(x => x.use(x => IO.succeed(x)))),
    test("Extra: String Equality Test")(assert("aaa\n111")(equalTo(aaa111))),
    testWriteFile("writeFileZManaged", writeFileZManaged.tupled.andThen(x => x.use(x => IO.succeed(x)))),
  )

  def testReadFile(funName: String, readFun: String => IO[Throwable, String]): ZSpec[Any, Throwable] =
    zio.test.suite(funName)(
      test(s"$funName(goodFile) succeeds") {
        for {
          res <- readFun(goodFile)
        } yield assert(res)(equalTo("hi\nbye"))
      },
      test(s"$funName(badFile) fails") {
        for {
          res <- readFun("badFile.txt").exit
        } yield assert(res)(fails(anything))
      },
    )

  def testWriteFile(
      funName: String,
      writeFun: ((String, String)) => IO[IOException, Unit],
  ): ZSpec[Any, IOException] = zio.test.suite(funName)(
    test(s"$funName(missingFile) succeeds") {
      // Note: test fails if "\n" is added to end of testStr
      val testStr = "test data out\nsecond line"
      for {
        resOut <- writeFun(missingFile, testStr).exit
        resIn  <- readFileZio(missingFile)
        _      <- ZIO.attempt(println(s"DEBUG: $resIn")).orDie
      } yield assert(resOut)(succeeds(anything)) && assert(resIn)(equalTo(testStr))
    },
  )
