package infrastructure.kafka.interpreter.`object`

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.scaladsl.{Source, SourceQueue}
import akka.stream.{OverflowStrategy, QueueCompletionResult, QueueOfferResult, UniqueKillSwitch}
import infrastructure.actor.ShardedActor
import infrastructure.actor.ShardedActor._
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.algebra.{KafkaTransaction, MessageProcessor, MessageProducer}
import infrastructure.kafka.consumer.TransactionalSource
import infrastructure.kafka.producer.TransactionalProducer
import infrastructure.serialization.algebra.{Deserializer, Serializer}

import scala.concurrent.{ExecutionContext, Future}

case class ObjectTransaction[Command <: Aggregate](
    from: String,
    to: String,
    actor: ShardedActor[Command],
    fromKafka: MessageProcessor[Command],
    toKafka: MessageProducer[Command]
) extends KafkaTransaction {
  def run(implicit ec: ExecutionContext) = {
    fromKafka.run(from, to) { command =>
      for {
        snapshot <- actor ask command
        published <- toKafka.producer(to) offer snapshot
      } yield {
        published match {
          case result: QueueCompletionResult => Right()
          case QueueOfferResult.Enqueued => Right()
          case QueueOfferResult.Dropped => Right()
        }
      }
    }
  }
}

object ObjectTransaction {
  def apply[Command <: Aggregate](actor: ShardedActor[Command], from: String, to: String)(
      implicit
      fromKafka: MessageProcessor[Command],
      toKafka: MessageProducer[Command]
  ): ObjectTransaction[Command] =
    ObjectTransaction(from, to, actor, fromKafka, toKafka)

  object Implicits {

    object KafkaTransaction {

      implicit def kafkaMessageConsumer[Command <: Aggregate](
          implicit
          deserializer: Deserializer[Command],
          serializer: Serializer[Command#Response],
          system: ActorSystem[Nothing],
          clusterSharding: ClusterSharding,
          ec: ExecutionContext,
          kafkaRequirements: KafkaRequirements
      ): MessageProcessor[Command] = new MessageProcessor[Command] {
        override def run(topic: String, group: String)(
            callback: Command => Future[Either[String, Unit]]
        ): (Option[UniqueKillSwitch], Future[Done]) = {
          val done = new TransactionalSource[Command]().run(topic, group) { callback }
          (Some(done._1), done._2)
        }
      }
      implicit def kafkaMessageProducer[Command <: Aggregate](
          implicit
          deserializer: Deserializer[Command],
          serializer: Serializer[Command#Response],
          system: ActorSystem[Nothing],
          clusterSharding: ClusterSharding,
          ec: ExecutionContext,
          kafkaRequirements: KafkaRequirements
      ): MessageProducer[Command] = new MessageProducer[Command] {
        override def producer(to: String): SourceQueue[Command#Response] =
          Source
            .queue(bufferSize = 1024, OverflowStrategy.backpressure)
            .to(new TransactionalProducer[Command#Response].sink(to)(serializer))
            .run
      }
    }

  }

}
