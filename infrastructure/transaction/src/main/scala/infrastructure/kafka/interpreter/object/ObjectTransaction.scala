package infrastructure.kafka.interpreter.`object`

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.scaladsl.{Source, SourceQueue}
import akka.stream.{OverflowStrategy, QueueCompletionResult, QueueOfferResult, UniqueKillSwitch}
import infrastructure.actor.ShardedActor
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.algebra.{KafkaTransaction, MessageProcessor, MessageProducer}
import infrastructure.kafka.consumer.TransactionalSource
import infrastructure.kafka.producer.TransactionalProducer
import infrastructure.serialization.algebra.{Deserializer, Serializer}
import scala.concurrent._

case class ObjectTransaction[Domain, Command, Response, ActorState](
    from: String,
    to: String,
    actor: ShardedActor[Command, ActorState],
    domainToCommand: (Domain, ActorRef[Response]) => Command,
    domainEntityIdExtractor: Domain => String,
    fromKafka: MessageProcessor[Domain],
    toKafka: MessageProducer[Response]
) extends KafkaTransaction {
  def run(implicit ec: ExecutionContext) = {
    fromKafka.run(from, to) { domain =>
      for {
        snapshot <- actor.ask[Response](domainEntityIdExtractor(domain))(ref => domainToCommand(domain, ref))
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
  def apply[Domain, Command, Response, ActorState](
      actor: ShardedActor[Command, ActorState],
      from: String,
      to: String
  )(
      implicit
      domainToCommand: (Domain, ActorRef[Response]) => Command,
      domainEntityIdExtractor: Domain => String,
      fromKafka: MessageProcessor[Domain],
      toKafka: MessageProducer[Response]
  ): ObjectTransaction[Domain, Command, Response, ActorState] =
    ObjectTransaction[Domain, Command, Response, ActorState](
      from = from,
      to = to,
      actor = actor,
      domainToCommand = domainToCommand,
      domainEntityIdExtractor = domainEntityIdExtractor,
      fromKafka = fromKafka,
      toKafka = toKafka
    )

  object Implicits { // TODO position this higher on the hierarchy. This does not belong only to ObjectTransaction, but to FunctionalTransaction as well.

    object KafkaTransaction {
      case class KafkaTransactionRequirements(
          clusterSharding: ClusterSharding,
          actorSystem: ActorSystem[Nothing],
          kafkaRequirements: KafkaRequirements,
          executionContext: ExecutionContext
      ) {
        def tupled = (clusterSharding, actorSystem, kafkaRequirements, executionContext)
        object Implicits {
          implicit def `sugar kafkaMessageConsumer`[Command](
              implicit
              deserializer: Deserializer[Command],
              requirements: KafkaTransactionRequirements
          ): MessageProcessor[Command] = {
            implicit val (clusterSharding, system, k, ec) = requirements.tupled
            kafkaMessageConsumer[Command]
          }
          implicit def `sugar kafkaMessageProducer`[Response](
              implicit
              serializer: Serializer[Response],
              requirements: KafkaTransactionRequirements
          ): MessageProducer[Response] = {
            implicit val (clusterSharding, system, k, ec) = requirements.tupled
            kafkaMessageProducer[Response]
          }
        }
      }

      implicit def kafkaMessageConsumer[Command](
          implicit
          deserializer: Deserializer[Command],
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
      implicit def kafkaMessageProducer[Response](
          implicit
          serializer: Serializer[Response],
          system: ActorSystem[Nothing],
          clusterSharding: ClusterSharding,
          ec: ExecutionContext,
          kafkaRequirements: KafkaRequirements
      ): MessageProducer[Response] = new MessageProducer[Response] {
        override def producer(to: String): SourceQueue[Response] =
          Source
            .queue(bufferSize = 1024, OverflowStrategy.backpressure)
            .to(new TransactionalProducer[Response].sink(to)(serializer))
            .run
      }
    }

  }

}
