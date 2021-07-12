package infrastructure.actor

import akka.actor.ActorSystem
import com.typesafe.config.Config

object System {

  def apply(
      name: String,
      config: Config
  ) = {
    ActorSystem(name, config)
  }

  case class ActorSystemRequirements(
      name: String,
      config: Config
  )
  def `with actor system`(proceed: ActorSystem => Unit)(
      implicit actorSystemRequirements: ActorSystemRequirements
  ): Unit =
    proceed(System(actorSystemRequirements.name, actorSystemRequirements.config))
}
