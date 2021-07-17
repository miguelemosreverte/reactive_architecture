val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

lazy val `monitoring` = project
  .settings(
    scalaVersion := "2.13.6"
  )
lazy val `kafka` = project
  .settings(
    scalaVersion := "2.13.6"
  )
  .dependsOn(serialization, actor, monitoring)
lazy val `websocket` = project.dependsOn(serialization, actor, kafka, http, monitoring)
lazy val `kafka_websocket` = project.dependsOn(kafka, websocket, monitoring)
lazy val `http_kafka` = project.dependsOn(kafka, http, monitoring)
lazy val `http` = project.dependsOn(actor, monitoring)
lazy val `actor` = project.dependsOn(monitoring)
lazy val `database` = project

lazy val `serialization` = project

name := "infrastructure"

lazy val infrastructure = project
  .in(file("."))
  .aggregate(
    actor,
    http,
    websocket,
    kafka_websocket,
    `http_kafka`,
    `kafka`,
    `serialization`,
    `monitoring`
  )

publishArtifact in GlobalScope in Test := true

// enable publishing the jar produced by `test:package`
publishArtifact in (Test, packageBin) := true

// enable publishing the test API jar
publishArtifact in (Test, packageDoc) := true

// enable publishing the test sources jar
publishArtifact in (Test, packageSrc) := true
