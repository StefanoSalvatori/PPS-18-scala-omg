name := "scala-omg"

version := "0.1"

// fork in Test := true

scalaVersion := "2.12.10"
val akkaVersion = "2.6.4"
val akkaHttpVersion = "10.1.11"
scalastyleFailOnWarning := true

// Test coverage
coverageEnabled := true
coverageExcludedPackages := ".*examples.*"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.1.1",
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  // "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.4" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % "2.6.4", // or whatever the latest version is
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "it.unibo.alice.tuprolog" % "tuprolog" % "3.1",
  "org.scalafx" %% "scalafx" % "12.0.2-R18",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test)

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux") => "linux"
  case n if n.startsWith("Mac") => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

// Add dependency on JavaFX libraries, OS dependent
lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map(m =>
  "org.openjfx" % s"javafx-$m" % "12.0.2" classifier osName


)


