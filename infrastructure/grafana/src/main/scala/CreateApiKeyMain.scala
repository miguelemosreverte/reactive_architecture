import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import infrastructure.http.Client
import infrastructure.http.Client.{POST, RequestError}
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

object CreateApiKeyMain extends App {
  implicit val actorSystem = ActorSystem("GETAPIKEY")
  implicit val executionContext = actorSystem.dispatcher
  implicit val http = new Client()

  import Client.Implicits._
  import stages.set_api_key.Domain._

  val post: POST[CreateApiKey, GetApiKeyResponse] = POST[CreateApiKey, GetApiKeyResponse](
    url = "http://0.0.0.0:3000/api/auth/keys",
    CreateApiKey(
      name = "apikeycurl20",
      role = "Admin"
    ),
    header = Some(Authorization(BasicHttpCredentials.apply("admin", "admin")))
  )
  println("Trying to do it")

  for {
    done <- post.response
  } yield {
    println(done.key)
  }

}
