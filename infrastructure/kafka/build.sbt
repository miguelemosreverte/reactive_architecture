import sbt.Keys._

organization := "infrastructure"
name := "kafka"
version := "1.0.0"

val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.1.0"

scalaVersion := scalaVer

libraryDependencies += ("com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion)
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.8" % Test
libraryDependencies += ("ch.qos.logback" % "logback-classic" % "1.2.3")
libraryDependencies += ("com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

fork / run := true
connectInput / run := true
