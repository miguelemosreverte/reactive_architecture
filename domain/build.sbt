import sbt.Keys._

name := "domain"
version := "1.0.0"

val scalaVer = "2.13.3"

scalaVersion := scalaVer

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.0-RC2"
