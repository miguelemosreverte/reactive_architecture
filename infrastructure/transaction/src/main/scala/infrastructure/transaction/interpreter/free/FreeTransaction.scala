package infrastructure.transaction.interpreter.free

import akka.Done
import akka.stream.UniqueKillSwitch
import infrastructure.transaction.algebra.KafkaTransaction

import scala.concurrent.{ExecutionContext, Future}

case class FreeTransaction(
    from: String,
    to: String,
    free_run: ExecutionContext => (Option[UniqueKillSwitch], Future[akka.Done])
) extends KafkaTransaction {
  override def run(implicit ec: ExecutionContext): (Option[UniqueKillSwitch], Future[Done]) = free_run(ec)
}
