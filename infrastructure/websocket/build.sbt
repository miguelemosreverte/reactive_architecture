import sbt.Keys._

organization := "infrastructure"
name := "websocket"
version := "1.0.0"

val scalaVer = "2.13.3"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
  "infrastructure" %% "kafka" % "1.0.0"
)

scalaVersion := scalaVer
