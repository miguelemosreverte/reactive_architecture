import sbt.Keys._

organization := "application"
version := "1.0.0"
name := "http_api"

val scalaVer = "2.13.3"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer
libraryDependencies ++= List(
  // menu
  "com.github.scopt" %% "scopt" % "4.0.1",
  // #deps
  "infrastructure" %% "http_kafka" % "1.0.0",
  "domain" %% "domain" % "1.0.0",
  // Logging
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

assemblyMergeStrategy in assembly := {
  case PathList("jackson-annotations-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-core-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-databind-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-dataformat-cbor-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-datatype-jdk8-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-datatype-jsr310-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-module-parameter-names-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-module-paranamer-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case _ => MergeStrategy.first

}

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
dockerExposedPorts ++= Seq(8081)

fork / run := true
connectInput / run := true
