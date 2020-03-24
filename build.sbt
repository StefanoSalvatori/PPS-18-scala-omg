name := "scala-omg"

version := "0.1"

scalaVersion := "2.12.10"
val akkaVersion = "2.6.4"

scalastyleFailOnWarning := true

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.4" % Test
)
