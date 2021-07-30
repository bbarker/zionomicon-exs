package chapter2

import zio.*
import java.io.IOException
import scala.io.BufferedSource

object Chapter2Exs:

  // 1
  def readFile(file: String): String =
    val source = scala.io.Source.fromFile(file)
    try source.getLines.mkString("\n") finally source.close()

  /** This is the "lazy way", and doesn't really allow ZIO-based
   * interrupts to happen inside the function. Also, there is
   * not as much flexibility in handling the flow and types
   * of errors internally in the function.
  **/
  def readFileZioNaive(file: String): IO[Throwable, String] =
    ZIO.attempt(readFile(file))

  def readFileZio(file: String): IO[IOException, String] = for {
    source <- IO.attempt(scala.io.Source.fromFile(file)).mapError(err => err match {
      case ex: IOException =>
        new IOException(s"Error in readFileZio opening $file: ${ex.getMessage}")
    })
    stringOut <- IO.attempt(source.getLines.mkString("\n")).mapError(err => err match {
      case ex: IOException =>
        new IOException(s"Error in readFileZio: getLines $file: ${ex.getMessage}")
    })
    _ <- IO.attempt(source.close()).mapError(err => err match {
      case ex: IOException =>
        new IOException(s"Error in readFileZio closing $file: ${ex.getMessage}")
    })
  } yield stringOut

  def readFileZManaged(file: String): Managed[IOException, String] =
    val acquire: ZIO[Any, IOException, BufferedSource] = IO.attempt(scala.io.Source.fromFile(file))
      .mapError(err => err match {
        case ex: IOException =>
          new IOException(s"Error in readFileZManaged opening $file: ${ex.getMessage}")
      })
    val release: BufferedSource => UIO[Unit] = source => ZIO.attempt(source.close()).orDie
    val managed = ZManaged.acquireReleaseWith(acquire)(release)
    managed.flatMap(src => ZIO.attempt(src.getLines.mkString("\n")).mapError(err => err match {
      case ex: IOException =>
        new IOException(s"Error in readFileZManaged: getLines $file: ${ex.getMessage}")
    }).toManaged)

  // 2
  def writeFile(file: String, text: String): Unit =
    import java.io.*
    val pw = new PrintWriter(new File(file))
    try pw.write(text) finally pw.close

  def writeFileZManaged(file: String, text: String): Managed[IOException, Unit] =
    import java.io.*
    val acquire: IO[IOException, PrintWriter] = IO.attempt(PrintWriter(new File(file)))
      .mapError(err => err match {
          case ex: IOException =>
            new IOException(s"Error in writeFileZManaged opening $file: ${ex.getMessage}")
        })
    val release: PrintWriter => UIO[Unit] = pw => ZIO.attempt(pw.close).orDie
    val managed = ZManaged.acquireReleaseWith (acquire)(release)
    managed.flatMap(pw => ZIO.attempt(pw.write(text)).mapError(err => err match {
      case ex: IOException =>
        new IOException(s"Error in writeFileZManaged: getLines $file: ${ex.getMessage}")
    }).toManaged)

