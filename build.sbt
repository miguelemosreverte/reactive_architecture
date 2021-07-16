name := "JitpackExperiment"

version := "0.1"

scalaVersion := "2.13.6"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.miguelemosreverte.reactive_architecture" % "serialization_2.13" % "main-130eed0b78-1" % "compile->compile;test->test"
libraryDependencies += "com.github.miguelemosreverte.reactive_architecture" % "actor_2.13" % "main-130eed0b78-1" % "compile->compile;test->test"
libraryDependencies += "com.github.miguelemosreverte.reactive_architecture" % "kafka_2.13" % "main-130eed0b78-1" % "compile->compile;test->test"

import sbt.Keys._

// #deps
val AkkaVersion = "2.6.15"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

libraryDependencies += ("com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion)
libraryDependencies += ("com.typesafe.akka" %% "akka-slf4j" % AkkaVersion)
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.8" % Test
libraryDependencies += ("ch.qos.logback" % "logback-classic" % "1.2.3")
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion
//libraryDependencies += ("infrastructure" %% "kafka" % "1.0.0" % "compile->compile;test->test")
//libraryDependencies += ("domain" %% "domain" % "1.0.0")
