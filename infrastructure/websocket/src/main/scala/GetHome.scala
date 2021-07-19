import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import infrastructure.http.Client
import infrastructure.http.Client.{GET, POST}

object GetHome extends App {
  implicit val actorSystem = ActorSystem("GETAPIKEY")
  implicit val executionContext = actorSystem.dispatcher
  implicit val http = new Client()

  import Client.Implicits._
  import stages.set_api_key.Domain._

  val get: GET[GetApiKeyResponse] = GET[GetApiKeyResponse](
    url = "http://0.0.0.0:3000/api/dashboards/home",
    header = Some(
      Authorization(
        OAuth2BearerToken.apply("eyJrIjoieUhFSVJOd0IwTnNJQUZLMG40ckkwaVcyb1N5SDBaRkUiLCJuIjoicGVwZSIsImlkIjoxfQ==")
      )
    )
  )
  println("Trying to do it")

  for {
    done <- get.response.value
  } yield {
    println(done)
  }

}
