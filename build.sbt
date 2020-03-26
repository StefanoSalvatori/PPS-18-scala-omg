name := "scala-omg"

version := "0.1"

scalaVersion := "2.12.10"
val akkaVersion = "2.6.4"
val akkaHttpVersion = "10.1.11"
scalastyleFailOnWarning := true

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.4" % Test,
  "com.typesafe.akka" %% "akka-stream" % "2.5.26", // or whatever the latest version is
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.3"

)


