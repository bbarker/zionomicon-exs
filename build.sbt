name := "zionomicon-exercises"

version := "0.1"

scalaVersion := "3.0.0"


scalacOptions ++= List(
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings", // Fail on warnings, not just errors
  // "-Vimplicits",
  // "-Vtype-diffs",
  "-source:future",   // Choices: future and future-migration. I use this to force future deprecation warnings, etc.
  "-new-syntax",      // Require `then` and `do` in control expressions.
)

libraryDependencies ++= {
  val zio  = "dev.zio"
  val zioV = "1.0.9"
  List(
    zio                    %% "zio"                 % zioV,
    zio                    %% "zio-streams"         % zioV,
    zio                    %% "zio-interop-cats"    % "3.1.1.0",
    // "co.fs2"               %% "fs2-core"            % "3.0.4",
    // "org.tpolecat"         %% "doobie-core"         % "1.0.0-M5",
    // "org.tpolecat"         %% "doobie-h2"           % "1.0.0-M5",
    // "com.github.fd4s"      %% "fs2-kafka"           % "2.1.0",
    // "org.http4s"           %% "http4s-blaze-server" % "1.0.0-M23",
    // "org.http4s"           %% "http4s-dsl"          % "1.0.0-M23",
    zio                    %% "zio-test"            % zioV % Test,
    zio                    %% "zio-test-sbt"        % zioV % Test,
    // ("io.github.kitlangton" %% "zio-magic"           % "0.3.5").cross(CrossVersion.for3Use2_13),
    // "ch.qos.logback"        % "logback-classic"     % "1.2.3"
  )
}

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
