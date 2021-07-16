package io.scalac.auction

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.cluster.sharding.typed.{javadsl, scaladsl, ShardingEnvelope}
import akka.stream.{KillSwitch, OverflowStrategy, QueueCompletionResult, QueueOfferResult, UniqueKillSwitch}
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.consumer.TransactionalSource
import infrastructure.kafka.producer.TransactionalProducer
import infrastructure.serialization.algebra._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.internal.EntityTypeKeyImpl
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.util.Timeout
import infrastructure.kafka.algebra.{MessageProcessor, MessageProducer}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.reflect.ClassTag

trait Aggregate {
  def id: String
  type Response
}

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

case class Message[Command, Response](command: Command, ref: ActorRef[Response])

trait MessageProcessor[Command] {

  def run(topic: String, group: String)(
      callback: Command => Future[Either[String, Unit]]
  ): (Option[UniqueKillSwitch], Future[Done])

}
trait MessageProducer[Command <: Aggregate] {
  def producer: SourceQueueWithComplete[Command#Response]
}

case class FromTo(from: String, to: String)
case class ActorTransactionDTO[Command <: Aggregate](
    from: String,
    to: String,
    actor: ShardedActor[Command],
    fromKafka: MessageProcessor[Command],
    toKafka: MessageProducer[Command]
)
object ActorTransactionDTO {
  def apply[Command <: Aggregate](from: String, to: String, actor: ShardedActor[Command])(
      implicit
      fromKafka: MessageProcessor[Command],
      toKafka: MessageProducer[Command]
  ) =
    ActorTransactionDTO(from, to, actor, fromKafka, toKafka)

}
case class ActorTransaction[Command <: Aggregate](from: String, to: String, actor: ShardedActor[Command])(
    implicit
    deserializer: Deserializer[Command],
    serializer: Serializer[Command#Response],
    system: ActorSystem[Nothing],
    clusterSharding: ClusterSharding,
    ec: ExecutionContext,
    kafkaRequirements: KafkaRequirements
) extends MessageProcessor[Command]
    with MessageProducer[Command] {
  implicit val timeout: Timeout = 20.seconds

  lazy val producer: SourceQueueWithComplete[Command#Response] =
    Source
      .queue(bufferSize = 1024, OverflowStrategy.backpressure)
      .to(new TransactionalProducer[Command#Response].sink(to)(serializer))
      .run

  def run: (UniqueKillSwitch, Future[Done]) =
    new TransactionalSource[Command]().run(from, "consumer") { command =>
      for {
        snapshot <- actor ask command
        published <- producer offer snapshot
      } yield {
        published match {
          case result: QueueCompletionResult => Right()
          case QueueOfferResult.Enqueued => Right()
          case QueueOfferResult.Dropped => Right()
        }
      }
    }

  override def run(topic: String, group: String)(
      callback: Command => Future[Either[String, Unit]]
  ): (Option[UniqueKillSwitch], Future[Done]) = ???
}
