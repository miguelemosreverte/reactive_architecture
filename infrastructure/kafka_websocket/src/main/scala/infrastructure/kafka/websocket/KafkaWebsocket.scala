package infrastructure.kafka.websocket

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source, SourceQueue}
import akka.stream.{OverflowStrategy, UniqueKillSwitch}
import chat.Websocket
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.serialization.algebra.Serialization

import scala.concurrent.ExecutionContext

object KafkaWebsocket {

  def apply[Event](ffromKafka: Source[Event, UniqueKillSwitch], ttoKafka: Sink[Event, NotUsed])(
      implicit
      kafkaRequirements: KafkaRequirements,
      serializer: Serialization[Event],
      actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ): Websocket[Event, Event] = {

    def fromKafka: Source[Event, NotUsed] =
      ffromKafka
        .toMat(BroadcastHub.sink(bufferSize = 1024))(Keep.right)
        .run()

    def toKafka: SourceQueue[Event] =
      Source.queue(bufferSize = 1024, OverflowStrategy.backpressure).to(ttoKafka).run

    new Websocket[Event, Event](
      fromWebsocket = toKafka,
      toWebsocket = fromKafka
    )

  }

}
