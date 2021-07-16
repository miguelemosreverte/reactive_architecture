import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpecLike, AsyncWordSpec}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.scalac.auction.lot.Domain.{Bid, Lot, LotId, User, UserId}
import io.scalac.auction.auction.actor.{Actor => AuctionActor}
import io.scalac.auction.lot.actor.{Actor => LotActor}
import io.scalac.auction.auction.Domain.{Auction, AuctionId}
import io.scalac.auction.auction.{Protocol => AuctionProtocol}
import io.scalac.auction.lot.{Protocol => LotProtocol}
import scala.concurrent.duration.DurationInt

class AuctionActorSpec extends AsyncWordSpec with Matchers {
  import akka.actor.typed.scaladsl.adapter._

  implicit lazy val system = ActorSystem(
    "auctionspec",
    ConfigFactory.parseString("""
      |
      |akka {
      |  actor {
      |    provider = cluster
      |    debug {
      |      receive = on
      |    }
      |  }
      |
      |  remote {
      |    artery {
      |      transport = tcp # See Selecting a transport below
      |      canonical.hostname = "0.0.0.0"
      |      canonical.port = 2552
      |    }
      |  }
      |
      |  cluster {
      |    seed-nodes = ["akka://auctionspec@0.0.0.0:2552"]
      |  }
      |
      |  extensions = ["akka.cluster.pubsub.DistributedPubSub"]
      |
      |
      |}
      |""".stripMargin)
  ).toTyped

  val exLot1 = Lot(LotId("a"))
  val exLot2 = Lot(LotId("b"))

  implicit lazy val sharding = ClusterSharding.apply(system)

  implicit val lot: ActorRef[ShardingEnvelope[io.scalac.auction.lot.Protocol.Command]] =
    new LotActor().sharded

  val auction: ActorRef[ShardingEnvelope[io.scalac.auction.auction.Protocol.Command]] =
    new AuctionActor().sharded

  import akka.actor.typed.scaladsl.AskPattern._
  implicit val ec = system.classicSystem.dispatcher
  implicit val timeout: Timeout = 20.seconds

  "AuctionActor" should {

    val user0 = User(UserId("user-0"))
    val user1 = User(UserId("user-1"))
    "add lot" in {
      for {
        response <- auction ask (
            (ref: ActorRef[AuctionProtocol.Response]) =>
              ShardingEnvelope.apply("1", AuctionProtocol.Commands.`add lot`(AuctionId("1"), exLot1, ref))
          )
        _ = println(response)
      } yield response should be(AuctionProtocol.Responses.Successes.`added lot`)
    } /*
    "list single lot" in {
      auction ! AuctionActor.List(probe.ref)
      probe.expectMessage(AuctionActor.Ok(Some(exLot1.toString)))
    }
    "remove lot" in {
      auction ! AuctionActor.Remove(exLot1, probe.ref)
      probe.expectMessage(AuctionActor.Ok())
    }
    "list lots if empty" in {
      auction ! AuctionActor.List(probe.ref)
      probe.expectMessage(AuctionActor.Ok(Some("")))
    }
    "add lots" in {
      auction ! AuctionActor.Add(exLot1, probe.ref)
      probe.expectMessage(AuctionActor.Ok())
      auction ! AuctionActor.Add(exLot2, probe.ref)
      probe.expectMessage(AuctionActor.Ok())
    }
     */
    "start" in {
      for {
        response <- auction ask (
            (ref: ActorRef[AuctionProtocol.Response]) =>
              ShardingEnvelope.apply("1", AuctionProtocol.Commands.Start(AuctionId("1"), ref))
          )
        _ = println(response)
      } yield response should be(AuctionProtocol.Responses.Successes.Started(AuctionId("1")))
    }
    "not restart" in {
      for {
        response <- auction ask (
            (ref: ActorRef[AuctionProtocol.Response]) =>
              ShardingEnvelope.apply("1", AuctionProtocol.Commands.Start(AuctionId("1"), ref))
          )
        _ = println(response)
      } yield response should be(AuctionProtocol.Responses.Failures.`cannot start auction again`)
    }
    "try a bid" in {
      for {
        response <- auction ask (
            (ref: ActorRef[LotProtocol.Response]) =>
              ShardingEnvelope.apply("1",
                                     AuctionProtocol.Commands.`bid lot`(AuctionId("1"), Bid(user0, exLot1, 5), ref))
          )
        _ = println(response)
      } yield response should be(LotProtocol.Responses.Successes.BidSuccessful(5, true))
    }
    "try an invalid bid" in {

      for {
        response <- auction ask (
            (ref: ActorRef[LotProtocol.Response]) =>
              ShardingEnvelope.apply("1",
                                     AuctionProtocol.Commands
                                       .`bid lot`(AuctionId("1"), Bid(user0, exLot1, -5), ref))
          )
        _ = println(response)
      } yield response should be(LotProtocol.Responses.Failures.`bid must be positive`)

    }
    "try a higher bid" in {
      for {
        response <- auction ask (
            (ref: ActorRef[LotProtocol.Response]) =>
              ShardingEnvelope.apply("1",
                                     AuctionProtocol.Commands
                                       .`bid lot`(AuctionId("1"), Bid(user1, exLot1, 10), ref))
          )
        _ = println(response)
      } yield response should be(LotProtocol.Responses.Successes.BidSuccessful(10, true))

    } /*
    "bid lower than the max of others" in {
      val bidder = testKit.createTestProbe[LotActor.BidResult]()
      auction ! AuctionActor.BidOn(exLot1, user0, 8, bidder.ref)
      bidder.expectMessage(LotActor.BidSuccessful(false))
    }*/

    "end" in {
      for {
        response <- auction ask (
            (ref: ActorRef[AuctionProtocol.Response]) =>
              ShardingEnvelope.apply("1", AuctionProtocol.Commands.End(AuctionId("1"), ref))
          )
        _ = println(response)
      } yield response should be(AuctionProtocol.Responses.Successes.Ended(AuctionId("1")))
    }
  }
}
