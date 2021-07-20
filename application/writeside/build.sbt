import sbt.Keys._

organization := "application"
version := "1.0.0"
name := "writeside"

val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

libraryDependencies += ("com.typesafe.akka" %% "akka-actor-typed" % "2.6.15")
libraryDependencies += ("com.typesafe.akka" %% "akka-slf4j" % "2.6.15")
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.15" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.8" % Test
libraryDependencies += ("ch.qos.logback" % "logback-classic" % "1.2.3")
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % "2.6.15"
libraryDependencies += ("infrastructure" %% "kafka" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("infrastructure" %% "transaction" % "1.0.0")
libraryDependencies += ("infrastructure" %% "microservice" % "1.0.0")
libraryDependencies += ("domain" %% "domain" % "1.0.0")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

fork / run := true
connectInput / run := true
