import sbt.Keys._

organization := "application"
version := "1.0.0"
name := "auditor"

val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

libraryDependencies += ("domain" %% "domain" % "1.0.0")
libraryDependencies += ("infrastructure" %% "microservice" % "1.0.0")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

fork / run := true
connectInput / run := true
