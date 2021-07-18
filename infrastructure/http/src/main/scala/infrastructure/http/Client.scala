package infrastructure.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes}
import akka.util.ByteString
import infrastructure.http.`Akka HTTP Client`.RequestError
import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`
import infrastructure.serialization.algebra.{Deserializer, Serialization}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Either

class Client()(
    implicit
    system: ActorSystem,
    executionContext: ExecutionContext,
    basicHttpCredentials: BasicHttpCredentials
) {

  final def GET[A](
      url: String,
      withHeader: HttpHeader = Authorization(
        basicHttpCredentials
      )
  )(implicit s: Serialization[A]): Future[Either[RequestError, A]] = {
    for {
      response: HttpResponse <- Http().singleRequest(
        akka.http.scaladsl.client.RequestBuilding
          .Get(url)
          .withHeaders(
            withHeader
          )
      )
      code = response.status
      entity: String <- `download HTTP entity`(response)
    } yield {
      code match {
        case akka.http.scaladsl.model.StatusCodes.OK =>
          s deserialize entity match {
            case Left(value: Deserializer.`failed to deserialize`) =>
              Left(Left(value))
            case Right(value: A) =>
              Right(value)
          }
        case error: akka.http.scaladsl.model.StatusCode =>
          Left(Right(error))
      }
    }
  }

  private def `download HTTP entity`(httpResponse: HttpResponse): Future[String] =
    httpResponse.entity.dataBytes
      .runFold(ByteString(""))(_ ++ _)
      .map(_.utf8String)

}

object Client {

  type RequestError = Either[`failed to deserialize`, akka.http.scaladsl.model.StatusCode]
}
