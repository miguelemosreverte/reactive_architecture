package infrastructure.transaction.interpreter.functional

import akka.stream.{QueueCompletionResult, QueueOfferResult}
import infrastructure.kafka.algebra.{MessageProcessor, MessageProducer}
import infrastructure.transaction.algebra.KafkaTransaction

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

case class FunctionalTransaction[Domain, Response](
    from: String,
    to: String,
    transaction: Domain => Future[Response],
    fromKafka: MessageProcessor[Domain],
    toKafka: MessageProducer[Response]
) extends KafkaTransaction {
  def run(implicit ec: ExecutionContext) = {
    fromKafka.run(from, to) { domain =>
      for {
        snapshot <- transaction(domain)
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
  def apply[Domain, Response](transaction: Domain => Future[Response], from: String, to: String)(
      implicit
      fromKafka: MessageProcessor[Domain],
      toKafka: MessageProducer[Response]
  ): FunctionalTransaction[Domain, Response] =
    FunctionalTransaction(from, to, transaction, fromKafka, toKafka)
}
