package chapter2

import zio.*
import java.io.IOException
import scala.io.BufferedSource

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
    _ <- writeFileZManaged(dest, contents)
  } yield ()

  // 4
  def printLine(line: String) = IO.attempt(println(line))
  val readLine = IO.attempt(scala.io.StdIn.readLine())

  val greetFun: IO[Throwable, Unit] =
    printLine("What is your name?").flatMap(_ =>
      readLine.flatMap(name =>
        printLine(s"Hello, ${name}!")))
  val greetFunFor: IO[Throwable, Unit] = for {
    _ <- printLine("What is your name?")
    name <- readLine
    _ <- printLine(s"Hello, $name!")
  } yield ()

  // 5
  val random = ZIO.attempt(scala.util.Random.nextInt(3) + 1)

  val guessingGame: IO[Throwable, Unit] =
    random.flatMap { int =>
      printLine("Guess a number:").flatMap { _ =>
        readLine.flatMap { num =>
          if num == int.toString then printLine("You guessed right!")
          else printLine(s"You guessed wrong, the number was $int!")
        }
      }
    }
  val guessingGameFor: IO[Throwable, Unit] = for {
    int <- random
    _ <- printLine("Guess a number:")
    num <- readLine
    _ <- if num == int.toString then printLine("You guessed right!")
         else printLine(s"You guessed wrong, the number was $int!")
  } yield ()

  // 6
  // TODO: probably move this out to another module
  final case class MyIO[-R, +E, +A](run: R => Either[E, A]):
    def map[B](f: A => B): MyIO[R, E, B] = MyIO(run.andThen(_.map(f)))
    def flatMap[R1 <: R, E1 >: E, B](f: A => MyIO[R1, E1, B]): MyIO[R1, E1, B] = MyIO(env =>
      run(env).map(f) match
        case Right(myIO) => myIO.run(env)
        case err@Left(e) => Left(e)
    )

  object MyIO:
    def pure[R, E, A](x: A): MyIO[R, E, A] = MyIO(_ => Right(x))
    def fail[R, E, A](e: E): MyIO[R, E, A] = MyIO(_ => Left(e))

    def putStrLn(string: String): MyIO[Any, IOException, Unit] =
      MyIO(_ => Right(println(string)))


    def zipWith[R, E, A, B, C](
      self: MyIO[R, E, A],
      that: MyIO[R, E, B]
    )(f: (A, B) => C): MyIO[R, E, C] = for {
      r1 <- self
      r2 <- that
    } yield f(r1, r2)


