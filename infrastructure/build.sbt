val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

lazy val `kafka` = project.dependsOn(serialization, actor)
lazy val `websocket` = project.dependsOn(serialization, actor, http)
lazy val `kafka_websocket` = project.dependsOn(kafka, websocket)
lazy val `http_kafka` = project.dependsOn(kafka, http)
lazy val `http` = project.dependsOn(actor)
lazy val `actor` = project
lazy val `database` = project

lazy val `serialization` = project

name := "infrastructure"

lazy val infrastructure = project
  .in(file("."))
  .aggregate(
    `kafka`,
    `serialization`
  )
