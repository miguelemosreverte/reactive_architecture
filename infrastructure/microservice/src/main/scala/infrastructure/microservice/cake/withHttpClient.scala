package infrastructure.microservice.cake

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.typesafe.config.ConfigFactory
import infrastructure.http.Client
import infrastructure.microservice.Microservice

trait withHttpClient {
  self: Microservice with withSystem =>

  implicit lazy val httpClient = new Client()(system, system.dispatcher)
}
