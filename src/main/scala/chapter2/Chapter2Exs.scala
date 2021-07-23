package chapter2

import zio.*
import java.io.IOException

object Chapter2Exs:

  // 1
  def readFile(file: String): String =
    val source = scala.io.Source.fromFile(file)
    try source.getLines.mkString finally source.close()

  /** This is the "lazy way", and doesn't really allow ZIO-based
   * interrupts to happen inside the function. Also, there is
   * not as much flexibility in handling the flow and types
   * of errors internally in the function.
  **/
  def readFileZioNaive(file: String): IO[Throwable, String] =
    ZIO.effect(readFile(file))

  def readFileZio(file: String): IO[IOException, String] = for {
    source <- IO.effect(scala.io.Source.fromFile(file)).mapError(err => err match {
      case ex: IOException =>
        new IOException(s"Error in readFileZio opening $file: ${ex.getMessage}")
    })
    stringOut = source.getLines.mkString
    _ <- IO.effect(source.close()).mapError(err => err match {
      case ex: IOException =>
        new IOException(s"Error in readFileZio closing $file: ${ex.getMessage}")
    })
  } yield stringOut
