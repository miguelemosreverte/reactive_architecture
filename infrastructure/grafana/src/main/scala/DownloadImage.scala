import DownloadImage.GetImage
import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.stream.scaladsl.{FileIO, Flow, Source}
import akka.util.ByteString
import cats.data.EitherT
import infrastructure.http.Client
import infrastructure.http.Client.{GET, RequestError}
import infrastructure.serialization.algebra.Deserializer
import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`

import java.nio.file.Paths
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions
import scala.util.Either

object DownloadImage {

  case class GetImage(
      url: String = "http://0.0.0.0:3000",
      uid: String = "68ecf0a5-4ca1-4afa-86e7-b73c9b315257",
      name: String = "from",
      from: Long = (DateTime.now - 5.minutes.toMillis).clicks,
      to: Long = DateTime.now.clicks,
      theme: String = "light",
      panelId: Int = 4,
      width: Int = 1000,
      height: Int = 1000,
      tz: String = "America%2FCordoba"
  ) {
    override def toString: String =
      s"$url/render/d-solo/$uid/$name?orgId=1&from=$from&to=$to&theme=$theme&panelId=$panelId&width=$width&height=$height&tz=$tz"
  }

}
class DownloadImage(getImage: GetImage, filename: String)(
    implicit
    system: ActorSystem,
    executionContext: ExecutionContext,
    httpClient: Client,
    token: OAuth2BearerToken = OAuth2BearerToken.apply(
      "eyJrIjoiOGtVSjU1cjVqSkdialJFd1REdWVYV1BDVWk4ZDhSekEiLCJuIjoiYXBpa2V5Y3VybDIwIiwiaWQiOjF9"
    )
) {
  for {
    done <- httpClient.`GET bytes`(
      url = GetImage().toString,
      headers = Some(
        Authorization(token)
      )
    )
  } yield {
    println(done)
    Source.single(done).runWith(FileIO.toPath(Paths.get(filename)))
  }
}
