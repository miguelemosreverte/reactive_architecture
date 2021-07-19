import sbt.Keys._

organization := "infrastructure"
name := "tracing"
version := "1.0.0"

scalaVersion := "2.13.6"

libraryDependencies ++= List(
  // menu
  "com.github.scopt" %% "scopt" % "4.0.1",
  // deps
  "infrastructure" %% "http" % "1.0.0"
)
