package chapter2

import chapter2.Chapter2Exs.*

import zio.*
import zio.test.Assertion.*
import zio.test.*

object Chapter2ExsSpec extends DefaultRunnableSpec:
  val goodFile: String = getClass.getResource("/goodFile.txt").getPath

  def spec = suite("Chapter 2 Exercises")(
    testReadFile("readFileZioNaive", readFileZioNaive),
    testReadFile("readFileZio", readFileZio),
    testReadFile("readFileZManaged",
      readFileZManaged.andThen(x => x.use(x => IO.succeed(x)))
    ),
  )

  def testReadFile(funName: String, readFun: String => IO[Throwable, String]): ZSpec[Any, Throwable] =
    zio.test.suite(funName)(
    testM(s"$funName(goodFile) succeeds"){
      for {
        res <- readFun(goodFile)
      } yield assert(res)(equalTo("hi\nbye"))
    },
    testM(s"$funName(badFile) fails"){
      for {
        res <- readFun("badFile.txt").run
      } yield assert(res)(fails(anything))
    },
  )
