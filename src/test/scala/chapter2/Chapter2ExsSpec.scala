package chapter2

import chapter2.Chapter2Exs.*

import zio.*
import zio.test.Assertion.*
import zio.test.*
import zio.test.environment.TestConsole
import java.io.IOException
import scala.compiletime.ops.any

object Chapter2ExsSpec extends DefaultRunnableSpec:
  val goodFileTxt: String = "hi\nbye"
  val goodFile: String    = getClass.getResource("/goodFile.txt").getPath
  val missingFile: String = goodFile.replaceFirst("goodFile.txt", "missingFile.txt")
  val copiedFile: String  = goodFile.replaceFirst("goodFile.txt", "copiedFile.txt")

  val aaa111 = "aaa\n111"

  def spec = suite("Chapter 2 Exercises")(
    testReadFile("readFileZioNaive", readFileZioNaive),
    testReadFile("readFileZio", readFileZio),
    testReadFile("readFileZManaged", readFileZManaged.andThen(x => x.use(x => IO.succeed(x)))),
    test("Extra: String Equality Test")(assert("aaa\n111")(equalTo(aaa111))),
    testWriteFile("writeFileZManaged", writeFileZManaged.tupled.andThen(x => x.use(x => IO.succeed(x)))),
    testCopyFile("copyFileZManaged", copyFileZManaged.tupled.andThen(x => x.use(x => IO.succeed(x)))),
    testMyIoZipWith,
    testCollectAll,
    testForeach,
    testPrintLine,
    testCat,
  )

  def testReadFile(funName: String, readFun: String => IO[Throwable, String]): ZSpec[Any, Throwable] =
    zio.test.suite(funName)(
      test(s"$funName(goodFile) succeeds") {
        for {
          res <- readFun(goodFile)
        } yield assert(res)(equalTo(goodFileTxt))
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
    test(s"$funName(missingFile, _) succeeds") {
      // Note: test fails if "\n" is added to end of testStr
      val testStr = "test data out\nsecond line"
      for {
        resOut <- writeFun(missingFile, testStr).exit
        resIn  <- readFileZio(missingFile)
        // _      <- ZIO.attempt(println(s"DEBUG: $resIn")).orDie
      } yield assert(resOut)(succeeds(anything)) && assert(resIn)(equalTo(testStr))
    },
  )

  def testCopyFile(
      funName: String,
      copyFun: ((String, String)) => IO[IOException, Unit],
  ): ZSpec[Any, IOException] = zio.test.suite(funName)(
    test(s"$funName(goodFile, copiedFile) succeeds") {
      for {
        resCopy <- copyFun(goodFile, copiedFile).exit
        resIn   <- readFileZio(copiedFile)
      } yield assert(resCopy)(succeeds(anything)) && assert(resIn)(equalTo(goodFileTxt))
    },
  )

  val testMyIoZipWith: ZSpec[Any, Throwable] = zio.test.suite("MyIO.zipWith")(
    test(s"MyIO.zipWith(putStrLn, putStrLn) succeeds") {
      for {
        zipUnit <- ZIO.attempt(
          (MyIO.zipWith(MyIO.putStrLn("hi! "), MyIO.putStrLn("bye!"))((x, y) => s"$x$y")).run(()),
        )
      } yield assert(zipUnit)(equalTo(Right("()()")))
    },
  )

  val testCollectAll: ZSpec[Any, Throwable] = zio.test.suite("MyIO.collectAll")(
    test(s"MyIO.collectAll([putStrLn, putStrLn]) succeeds") {
      for {
        units <- ZIO.attempt(
          (MyIO
            .collectAll(
              List(MyIO.putStrLn("hi! "), MyIO.putStrLn("bye!")),
            ))
            .run(()),
        )
      } yield assert(units)(equalTo(Right(List((), ())))) // Note: equalTo(Right("List((), ())")) typechecks!
    },
  )

  def effectfulDouble[R](x: Int): MyIO[R, Nothing, Double] = MyIO.pure(2.0 * x)

  val testForeach: ZSpec[Any, Throwable] = zio.test.suite("MyIO.foreach")(
    test(s"MyIO.foreach([putStrLn, putStrLn]) succeeds") {
      val inSeq   = Seq(1, 2, 3)
      val outList = MyIO.foreach(inSeq)(effectfulDouble).run(())
      assert(outList)(equalTo(Right(List(2.0, 4.0, 6.0))))
    },
  )

  val testPrintLine: ZSpec[ZEnv & Has[TestConsole], Throwable] = zio.test.suite("printLine")(
    test(s"We can test ZIO's printLine") {
      for {
        _      <- Console.printLine("hello")
        _      <- Console.printLine("goodbye")
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector("hello\n", "goodbye\n")))
    },
  )

  val testCat: ZSpec[ZEnv & Has[TestConsole], Nothing] = zio.test.suite("cat")(
    test(s"Cat.run goodFile.txt succeeds") {
      for {
        code   <- Cat.run(List(goodFile, goodFile))
        output <- TestConsole.output
      } yield assert(code)(equalTo(ExitCode.success))
        && assert(output)(equalTo(Vector(goodFileTxt + "\n", goodFileTxt + "\n")))
    },
  )
