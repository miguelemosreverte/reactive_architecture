package infrastructure.kafka.interpreter.functional

import akka.stream.{QueueCompletionResult, QueueOfferResult}
import infrastructure.actor.ShardedActor._
import infrastructure.kafka.algebra.{KafkaTransaction, MessageProcessor, MessageProducer}

import scala.concurrent.{ExecutionContext, Future}

case class FunctionalTransaction[Command <: Aggregate](
    from: String,
    to: String,
    transaction: Command => Future[Command#Response],
    fromKafka: MessageProcessor[Command],
    toKafka: MessageProducer[Command]
) extends KafkaTransaction {
  def run(implicit ec: ExecutionContext) = {
    fromKafka.run(from, to) { command =>
      for {
        snapshot <- transaction(command)
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

object FunctionalTransaction {
  def apply[Command <: Aggregate](transaction: Command => Future[Command#Response], from: String, to: String)(
      implicit
      fromKafka: MessageProcessor[Command],
      toKafka: MessageProducer[Command]
  ): FunctionalTransaction[Command] =
    FunctionalTransaction(from, to, transaction, fromKafka, toKafka)
}
