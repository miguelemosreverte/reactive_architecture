package chat

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.handleWebSocketMessages
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl._
import infrastructure.serialization.algebra._

import scala.concurrent.ExecutionContext

class Websocket[In, Out](
    fromWebsocket: SourceQueue[In],
    toWebsocket: Source[Out, NotUsed]
)(
    implicit
    serializer: Serialization[Out],
    deserializer: Serialization[In],
    executionContext: ExecutionContext,
    actorSystem: ActorSystem
) {

  def route: Route = handleWebSocketMessages(flow)
  def flow: Flow[Message, Message, NotUsed] = {

    def serializeFlow: Flow[Out, Message, NotUsed] =
      Flow[Out]
        .map(e => TextMessage.Strict(serializer serialize (e, multiline = false)))

    def deserializeFlow: Flow[Message, In, NotUsed] =
      Flow[Message]
        .collect { case TextMessage.Strict(message) => deserializer deserialize message }
        .collect { case Right(value) => value }

    def queueWriter: Sink[In, NotUsed] =
      Flow[In]
        .mapAsync(1) { fromWebsocket.offer }
        .to(Sink.ignore)

    Flow.fromSinkAndSource(
      sink = deserializeFlow.to(queueWriter),
      source = toWebsocket.via(serializeFlow)
    )

  }
}
