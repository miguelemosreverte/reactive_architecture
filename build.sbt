lazy val root = project
  .in(file("."))
  .settings(
    name := "akka-auction-workshop",
    version := "0.1.0",
    scalaVersion := "2.13.6"
  ).aggregate(infrastructure)

lazy val `infrastructure` = project

lazy val `application` = project
  .dependsOn(domain)
  .dependsOn(infrastructure % "compile->compile;test->test")

lazy val `serialization` = (project in file(".") / "infrastructure" / "serialization")

lazy val `domain` = project
  .dependsOn(serialization)
  
  
publishArtifact in GlobalScope in Test := true

// enable publishing the jar produced by `test:package`
publishArtifact in (Test, packageBin) := true

// enable publishing the test API jar
publishArtifact in (Test, packageDoc) := true

// enable publishing the test sources jar
publishArtifact in (Test, packageSrc) := true
