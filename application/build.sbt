val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

name := "application"

lazy val `writeside` = project
lazy val `readside` = project
lazy val `websocket_api` = project
lazy val `http_api` = project

lazy val application = project
  .in(file("."))
  .aggregate(
    `readside`,
    `writeside`,
    `http_api`,
    `websocket_api`
  )
