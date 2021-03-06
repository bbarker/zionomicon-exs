package chapter2

import zio.*
import java.io.IOException
import java.lang.System
import scala.annotation.tailrec
import scala.io.BufferedSource

import canequal.all.given

object Chapter2Exs:

  // 1
  def readFile(file: String): String =
    val source = scala.io.Source.fromFile(file)
    try source.getLines.mkString("\n")
    finally source.close()

  /** This is the "lazy way", and doesn't really allow ZIO-based interrupts to happen inside the function. Also, there is not as much
    * flexibility in handling the flow and types of errors internally in the function.
    */
  def readFileZioNaive(file: String): IO[Throwable, String] =
    ZIO.attempt(readFile(file))

  def readFileZio(file: String): IO[IOException, String] = for {
    source <- IO
      .attempt(scala.io.Source.fromFile(file))
      .mapError(err =>
        err match {
          case ex: IOException =>
            new IOException(s"Error in readFileZio opening $file: ${ex.getMessage}")
        },
      )
    stringOut <- IO
      .attempt(source.getLines.mkString("\n"))
      .mapError(err =>
        err match {
          case ex: IOException =>
            new IOException(s"Error in readFileZio: getLines $file: ${ex.getMessage}")
        },
      )
    _ <- IO
      .attempt(source.close())
      .mapError(err =>
        err match {
          case ex: IOException =>
            new IOException(s"Error in readFileZio closing $file: ${ex.getMessage}")
        },
      )
  } yield stringOut

  def readFileZManaged(file: String): Managed[IOException, String] =
    val acquire: ZIO[Any, IOException, BufferedSource] = IO
      .attempt(scala.io.Source.fromFile(file))
      .mapError(err =>
        err match {
          case ex: IOException =>
            new IOException(s"Error in readFileZManaged opening $file: ${ex.getMessage}")
        },
      )
    val release: BufferedSource => UIO[Unit] = source => ZIO.attempt(source.close()).orDie
    val managed                              = ZManaged.acquireReleaseWith(acquire)(release)
    managed.flatMap(src =>
      ZIO
        .attempt(src.getLines.mkString("\n"))
        .mapError(err =>
          err match {
            case ex: IOException =>
              new IOException(s"Error in readFileZManaged: getLines $file: ${ex.getMessage}")
          },
        )
        .toManaged,
    )

  // 2
  def writeFile(file: String, text: String): Unit =
    import java.io.*
    val pw = new PrintWriter(new File(file))
    try pw.write(text)
    finally pw.close

  def writeFileZManaged(file: String, text: String): Managed[IOException, Unit] =
    import java.io.*
    val acquire: IO[IOException, PrintWriter] = IO
      .attempt(PrintWriter(new File(file)))
      .mapError(err =>
        err match {
          case ex: IOException =>
            new IOException(s"Error in writeFileZManaged opening $file: ${ex.getMessage}")
        },
      )
    val release: PrintWriter => UIO[Unit] = pw => ZIO.attempt(pw.close).orDie
    val managed                           = ZManaged.acquireReleaseWith(acquire)(release)
    managed.flatMap(pw =>
      ZIO
        .attempt(pw.write(text))
        .mapError(err =>
          err match {
            case ex: IOException =>
              new IOException(s"Error in writeFileZManaged: getLines $file: ${ex.getMessage}")
          },
        )
        .toManaged,
    )

  // 3
  def copyFile(source: String, dest: String): Unit =
    val contents = readFile(source)
    writeFile(dest, contents)

  def copyFileZManaged(source: String, dest: String): Managed[IOException, Unit] = for {
    contents <- readFileZManaged(source)
    _        <- writeFileZManaged(dest, contents)
  } yield ()

  // 4
  def myPrintLine(line: String) = IO.attempt(println(line))
  val readLine                  = IO.attempt(scala.io.StdIn.readLine())

  val greetFun: IO[Throwable, Unit] =
    myPrintLine("What is your name?").flatMap(_ => readLine.flatMap(name => myPrintLine(s"Hello, $name!")))
  val greetFunFor: IO[Throwable, Unit] = for {
    _    <- myPrintLine("What is your name?")
    name <- readLine
    _    <- myPrintLine(s"Hello, $name!")
  } yield ()

  // 5
  val random = ZIO.attempt(scala.util.Random.nextInt(3) + 1)

  val guessingGame: IO[Throwable, Unit] =
    random.flatMap { int =>
      myPrintLine("Guess a number:").flatMap { _ =>
        readLine.flatMap { num =>
          if num == int.toString then myPrintLine("You guessed right!")
          else myPrintLine(s"You guessed wrong, the number was $int!")
        }
      }
    }
  val guessingGameFor: IO[Throwable, Unit] = for {
    int <- random
    _   <- myPrintLine("Guess a number:")
    num <- readLine
    _ <-
      if num == int.toString then myPrintLine("You guessed right!")
      else myPrintLine(s"You guessed wrong, the number was $int!")
  } yield ()

  // 6
  // TODO: probably move this out to another module
  final case class MyIO[-R, +E, +A](run: R => Either[E, A]):
    def map[B](f: A => B): MyIO[R, E, B] = MyIO(run.andThen(_.map(f)))
    def flatMap[R1 <: R, E1 >: E, B](f: A => MyIO[R1, E1, B]): MyIO[R1, E1, B] = MyIO(env =>
      run(env).map(f) match
        case Right(myIO)   => myIO.run(env)
        case err @ Left(e) => Left(e),
    )

  object MyIO:
    def pure[R, E, A](x: => A): MyIO[R, E, A] = MyIO(_ => Right(x))
    def fail[R, E, A](e: => E): MyIO[R, E, A] = MyIO(_ => Left(e))

    def putStrLn(string: String): MyIO[Any, IOException, Unit] =
      MyIO(_ => Right(println(string)))

    def zipWith[R, E, A, B, C](
        self: MyIO[R, E, A],
        that: MyIO[R, E, B],
    )(f: (A, B) => C): MyIO[R, E, C] = for {
      r1 <- self
      r2 <- that
    } yield f(r1, r2)

    // 7
    // This is basically "sequence"
    /*
    // I like the tailrec implementation more, but not entirely sure why
    // @nowarn doesn't work
    @nowarn // ("msg=match may not be exhaustive")
    def collectAll[R, E, A](in: Iterable[MyIO[R, E, A]]): MyIO[R, E, List[A]] =
      MyIO(
        env => {
          val resList: List[Either[E,A]] = in.map(io => io.run(env)).toList
          val failures = resList.filter(_.isLeft)
          val successes = resList.filter(_.isRight)
          (failures match {
            case (fHead::_) => MyIO.fail(fHead match {case Left(err) => err})
            case Nil => MyIO.pure(successes.map{case Right(x) => x})
          }).run(env)
        }
      )
     */
    val ints = List(1, 2, 3)

    ints match {
      case x :: _ => println(x)
      case Nil    => println("it's empty")
    }

    val intOpts: List[Option[Int]] = List(Some(1), None, Some(3))
    intOpts match {
      case x :: _ => println(x)
      case Nil    => println("it's empty")
    }

    val intIOs: List[MyIO[Any, Unit, Int]] = List(
      MyIO.pure(1),
      MyIO.fail(()),
      MyIO.pure(3),
    )
    intIOs match {
      case x :: _ => println(x)
      case List() => println("it's empty")
    }

    /*
     * Like an effect-specific `sequence` from Haskell
     */
    def collectAll[R, E, A](in: Iterable[MyIO[R, E, A]]): MyIO[R, E, List[A]] =
      @tailrec
      def go(
          env: R,
          ioList: List[MyIO[R, E, A]],
          builder: List[A],
      ): Either[E, List[A]] =
        ioList match {
          case io :: ios =>
            io.run(env) match {
              case Left(err) => Left(err)
              case Right(x)  => go(env, ios, x :: builder)
            }
          case List() => Right(builder)
        }
      MyIO(env => go(env, in.toList, Nil)).map(_.reverse)

    // 8
    /*
     * Like an effect-specific `traverse` from Haskell
     * `collectAll` is very similar, structurally, and could be written
     * in terms of `foreach`.
     */
    def foreach[R, E, A, B](
        in: Iterable[A],
    )(f: A => MyIO[R, E, B]): MyIO[R, E, List[B]] =
      @tailrec
      def go(
          env: R,
          ioList: List[MyIO[R, E, A]],
          builder: List[B],
      ): Either[E, List[B]] =
        ioList match {
          case io :: ios =>
            io.flatMap(f).run(env) match {
              case Left(err) => Left(err)
              case Right(x)  => go(env, ios, x :: builder)
            }
          case List() => Right(builder)
        }
      MyIO(env => go(env, in.toList.map(MyIO.pure), Nil)).map(_.reverse)

    // 9, untested
    def orElse[R, E1, E2, A](
        self: MyIO[R, E1, A],
        that: MyIO[R, E2, A],
    ): MyIO[R, E2, A] = MyIO(env =>
      self.run(env) match {
        case Left(_)  => that.run(env)
        case Right(v) => Right(v)
      },
    )

  // 10
  import zio.App as ZIOApp
  object Cat extends ZIOApp {
    def run(commandLineArguments: List[String]) = ZManaged
      .foreach(commandLineArguments)(
        readFileZManaged,
      )
      .use(lines => ZIO.foreach(lines)(l => Console.printLine(l)))
      .exitCode
  }

  // 11
  def eitherToZIO[E, A](either: Either[E, A]): ZIO[Any, E, A] = either match {
    case Left(err)  => ZIO.fail(err)
    case Right(res) => ZIO.succeed(res)
  }

  // 12 (this is basically a safe `head` but for ZIO)
  def listToZIO[A](list: List[A]): ZIO[Any, None.type, A] = list match {
    case x :: xs => ZIO.succeed(x)
    case _       => ZIO.fail(None)
  }

  // 13
  def currentTime(): Long                          = System.currentTimeMillis()
  lazy val currentTimeZIO: ZIO[Any, Nothing, Long] = ZIO.succeed(System.currentTimeMillis())

  // 14
  //
  // Convert this call-back function to ZIO using effectAsync
  //
  def getCacheValue(
      key: String,
      onSuccess: String => Unit,
      onFailure: Throwable => Unit,
  ): Unit = ???

// def getCacheValueZIO(key: String, onSucc: String => Unit, onFail: Throwable => Unit): IO[Throwable, String] =
//   IO.async(register => register(getCacheValue(key, onSucc, onFail)))
