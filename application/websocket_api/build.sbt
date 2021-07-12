val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

organization := "application"
version := "1.0.0"
name := "websocket_api"

libraryDependencies += ("infrastructure" %% "kafka_websocket" % "1.0.0")
libraryDependencies += ("domain" %% "domain" % "1.0.0")
