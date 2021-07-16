package infrastructure.actor

import akka.actor.typed.scaladsl.AskPattern.{Askable, _}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.util.Timeout
import infrastructure.actor.ShardedActor._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.reflect.ClassTag

abstract class ShardedActor[Command <: Aggregate: ClassTag]()(
    implicit
    sharding: ClusterSharding,
    system: ActorSystem[Nothing],
    timeout: Timeout = Timeout(20 seconds)
) {
  type `Command to Event` = Behavior[Message[Command, Command#Response]]
  protected def behavior(id: String): `Command to Event`

  private final type M = Message[Command, Command#Response]
  private final def init(
      implicit
      sharding: ClusterSharding,
      system: ActorSystem[Nothing],
      timeout: Timeout = Timeout(20 seconds)
  ): ActorRef[ShardingEnvelope[M]] = {
    val entityTypeKey: EntityTypeKey[M] = EntityTypeKey.apply[M](implicitly[ClassTag[Command]].runtimeClass.getTypeName)
    val entityRef: ActorRef[ShardingEnvelope[M]] = sharding.init(Entity(entityTypeKey)(createBehavior = { context =>
      behavior(context.entityId)
    }))
    entityRef
  }
  private val shardActor = init

  final def ask(command: Command): Future[Command#Response] =
    shardActor.ask((actorRef: ActorRef[Command#Response]) =>
      ShardingEnvelope.apply(
        entityId = command.id,
        message = Message(command, actorRef)
      )
    )

}

object ShardedActor {
  case class Message[Command, Response](command: Command, ref: ActorRef[Response])

  trait Aggregate {
    def id: String
    type Response
  }
}
