organization := "application"
version := "1.0.0"
name := "documentation"

scalaVersion := "2.13.6"

libraryDependencies += ("infrastructure" %% "kafka" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("domain" %% "domain" % "1.0.0")
libraryDependencies += ("infrastructure" %% "tracing" % "1.0.0")
libraryDependencies += ("infrastructure" %% "grafana" % "1.0.0")
libraryDependencies += ("infrastructure" %% "microservice" % "1.0.0")
