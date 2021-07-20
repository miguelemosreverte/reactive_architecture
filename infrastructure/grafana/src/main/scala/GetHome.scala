import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import infrastructure.http.Client
import infrastructure.http.Client.{GET, POST}

object GetHome extends App {
  implicit val actorSystem = ActorSystem("GETAPIKEY")
  implicit val executionContext = actorSystem.dispatcher
  implicit val http = new Client()

  import stages.set_api_key.Domain._

  for {
    done <- http.GET[GetApiKeyResponse](
      url = "http://0.0.0.0:3000/api/dashboards/home",
      headers = Some(
        Authorization(
          OAuth2BearerToken.apply("eyJrIjoieUhFSVJOd0IwTnNJQUZLMG40ckkwaVcyb1N5SDBaRkUiLCJuIjoicGVwZSIsImlkIjoxfQ==")
        )
      )
    )
  } yield {
    println(done)
  }

}
