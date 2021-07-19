package infrastructure.microservice.cake

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.typesafe.config.ConfigFactory
import infrastructure.microservice.Microservice

trait withSystem {
  self: Microservice =>

  implicit lazy val system = ActorSystem(
    name,
    ConfigFactory
      .parseString(
        s"""
           |
           |akka {
           |  actor {
           |    provider = cluster
           |  }
           |
           |  remote {
           |    artery {
           |      transport = tcp # See Selecting a transport below
           |      canonical.hostname = "0.0.0.0"
           |      canonical.port = 2552
           |    }
           |  }
           |
           |  cluster {
           |    seed-nodes = ["akka://${name}@0.0.0.0:2552"]
           |  }
           |
           |}
           |""".stripMargin
      )
      .withFallback(ConfigFactory.load())
  )

  import akka.actor.typed.scaladsl.adapter._

  implicit lazy val typedSystem = system.toTyped

  implicit lazy val clusterSharding: ClusterSharding = ClusterSharding.apply(typedSystem)
}
