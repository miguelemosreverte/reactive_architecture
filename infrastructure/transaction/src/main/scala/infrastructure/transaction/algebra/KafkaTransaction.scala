package infrastructure.transaction.algebra

import akka.stream.UniqueKillSwitch

import scala.concurrent.{ExecutionContext, Future}

trait KafkaTransaction {
  def from: String
  def to: String
  def run(implicit ec: ExecutionContext): (Option[UniqueKillSwitch], Future[akka.Done])
}
