package infrastructure.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.util.ByteString
import cats.data.EitherT
import infrastructure.http.Client.{`http request with body`, `http request without body`, `http request`, RequestError}
import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`
import infrastructure.serialization.algebra.{Deserializer, Serialization, Serializer}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Either

object Client {
  type RequestError = Either[`failed to deserialize`, (akka.http.scaladsl.model.StatusCode, String)]

  sealed trait `http request`[Response] {
    val url: String
    val header: Option[HttpHeader]
  }
  sealed trait `http request without body`[Response] extends `http request`[Response] {
    val url: String
    val header: Option[HttpHeader]
  }
  sealed trait `http request with body`[Body, Response] extends `http request`[Response] {
    val url: String
    val body: Body
    val contentType: ContentType.WithFixedCharset = ContentTypes.`application/json`
    val header: Option[HttpHeader]
  }
  case class GET[Response](url: String, header: Option[HttpHeader] = None)(
      implicit
      responseDeserializer: Deserializer[Response]
  ) extends `http request without body`[Response]
  case class POST[Body, Response](url: String, body: Body, header: Option[HttpHeader] = None)(
      implicit
      bodySerializer: Serializer[Body],
      responseDeserializer: Deserializer[Response]
  ) extends `http request with body`[Body, Response]
  case class PUT[Body, Response](url: String, body: Body, header: Option[HttpHeader] = None)(
      implicit
      bodySerializer: Serializer[Body],
      responseDeserializer: Deserializer[Response]
  ) extends `http request with body`[Body, Response]
  case class DELETE[Response](url: String, header: Option[HttpHeader] = None)(
      implicit
      bodySerializer: Serializer[_],
      responseDeserializer: Deserializer[_]
  ) extends `http request without body`[Response]

  object Implicits {
    implicit class `http request without body sintactic sugar`[Response](
        request: `http request without body`[Response]
    ) {
      def response(
          implicit
          serialization: Serialization[Response],
          httpClient: Client
      ): EitherT[Future, RequestError, Response] = request match {
        case req: GET[Response] => httpClient.GET(req)
        case req: DELETE[Response] => httpClient.DELETE(req)
      }
    }

    implicit class `http request with body sintactic sugar`[Body, Response](
        request: `http request with body`[Body, Response]
    ) {
      def response(
          implicit
          deserialization: Serialization[Body],
          serialization: Serialization[Response],
          httpClient: Client
      ): EitherT[Future, RequestError, Response] = request match {
        case req: POST[Body, Response] => httpClient.POST(req)
        case req: PUT[Body, Response] => httpClient.PUT(req)
      }
    }
  }
}
class Client()(
    implicit
    system: ActorSystem,
    executionContext: ExecutionContext,
    basicHttpCredentials: Option[BasicHttpCredentials] = None
) {

  private final def GET[Response](hTTP_Request: `http request without body`[Response])(
      implicit
      outS: Serialization[Response]
  ): EitherT[Future, RequestError, Response] =
    http[Response](hTTP_Request, identity)

  private final def POST[Body, Response](hTTP_Request: `http request with body`[Body, Response])(
      implicit
      inS: Serialization[Body],
      outS: Serialization[Response]
  ): EitherT[Future, RequestError, Response] =
    http[Response](hTTP_Request, {
      _.withEntity(hTTP_Request.contentType, {
        val serialized = inS serialize hTTP_Request.body
        println("HTTP POST")
        println(serialized)
        serialized
      })
    })

  private final def PUT[Body, Response](hTTP_Request: `http request with body`[Body, Response])(
      implicit
      inS: Serialization[Body],
      outS: Serialization[Response]
  ): EitherT[Future, RequestError, Response] =
    http[Response](hTTP_Request, { _.withEntity(hTTP_Request.contentType, inS serialize hTTP_Request.body) })

  private final def DELETE[Response](hTTP_Request: `http request without body`[Response])(
      implicit
      outS: Serialization[Response]
  ): EitherT[Future, RequestError, Response] =
    http[Response](hTTP_Request, identity)

  private final def http[Response](
      hTTP_Request: `http request`[Response],
      addBody: HttpRequest => HttpRequest
  )(
      implicit
      outS: Serialization[Response]
  ): EitherT[Future, RequestError, Response] = {
    EitherT(
      for {
        response: HttpResponse <- Http().singleRequest {
          val requestBuilder: HttpRequest = (hTTP_Request match {
            case _: infrastructure.http.Client.GET[_] =>
              akka.http.scaladsl.client.RequestBuilding.Get
            case _: infrastructure.http.Client.POST[_, _] =>
              akka.http.scaladsl.client.RequestBuilding.Post
            case _: infrastructure.http.Client.PUT[_, _] =>
              akka.http.scaladsl.client.RequestBuilding.Put
            case _: infrastructure.http.Client.DELETE[_] =>
              akka.http.scaladsl.client.RequestBuilding.Delete
          }).apply(hTTP_Request.url)

          hTTP_Request.header match {
            case Some(header) =>
              addBody(requestBuilder).withHeaders(header)
            case None =>
              addBody(requestBuilder)
          }

        }
        code = response.status
        entity: String <- `download HTTP entity`(response)
      } yield {
        code match {
          case akka.http.scaladsl.model.StatusCodes.OK =>
            outS deserialize entity match {
              case Left(value: Deserializer.`failed to deserialize`) =>
                Left(Left(value))
              case Right(value: Response) =>
                Right(value)
            }
          case error: akka.http.scaladsl.model.StatusCode =>
            Left(Right(error, entity))
        }
      }
    )
  }

  private def `download HTTP entity`(httpResponse: HttpResponse): Future[String] =
    httpResponse.entity.dataBytes
      .runFold(ByteString(""))(_ ++ _)
      .map(_.utf8String)

}
