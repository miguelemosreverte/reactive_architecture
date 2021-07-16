package infrastructure.kafka.algebra

import akka.stream.scaladsl.SourceQueue
import infrastructure.actor.ShardedActor.Aggregate

trait MessageProducer[Command <: Aggregate] {
  def producer(to: String): SourceQueue[Command#Response]
}
