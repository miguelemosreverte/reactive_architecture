package infrastructure.kafka.interpreter.free

import akka.Done
import akka.stream.UniqueKillSwitch

import scala.concurrent.{ExecutionContext, Future}

case class FreeTransaction(
    from: String,
    to: String,
    free_run: ExecutionContext => (Option[UniqueKillSwitch], Future[akka.Done])
) extends infrastructure.kafka.algebra.KafkaTransaction {
  override def run(implicit ec: ExecutionContext): (Option[UniqueKillSwitch], Future[Done]) = free_run(ec)
}
