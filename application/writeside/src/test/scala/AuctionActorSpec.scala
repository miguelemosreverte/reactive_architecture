import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import io.scalac.auction.lot.Domain.{Bid, Lot, LotId, User, UserId}
import io.scalac.auction.auction.actor.{Actor => AuctionActor}
import io.scalac.auction.auction.Domain.{Auction, AuctionId}
import io.scalac.auction.auction.{Protocol => AuctionProtocol}
import io.scalac.auction.lot.{Protocol => LotProtocol}

import java.util.UUID

class AuctionActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {

  val exLot1 = Lot(LotId("a"))
  val exLot2 = Lot(LotId("b"))

  "AuctionActor" should {
    val auction = testKit.spawn(AuctionActor(Auction(AuctionId("1"), Set(exLot1, exLot2))), "auction-0")
    val probe = testKit.createTestProbe[AuctionProtocol.Response]()
    val user0 = User(UserId("user-0"))
    val user1 = User(UserId("user-1"))
    "add lot" in {
      auction ! AuctionProtocol.Commands.`add lot`(AuctionId("1"), exLot1, probe.ref)
      probe.expectMessage(AuctionProtocol.Responses.Successes.`added lot`)
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
      auction ! AuctionProtocol.Commands.Start(AuctionId("1"), probe.ref)
      probe.expectMessage(AuctionProtocol.Responses.Successes.Started(AuctionId("1")))
    }
    "not restart" in {
      auction ! AuctionProtocol.Commands.Start(AuctionId("1"), probe.ref)
      probe.expectMessage(AuctionProtocol.Responses.Failures.`cannot start auction again`)
    }
    "try a bid" in {
      val bidder = testKit.createTestProbe[LotProtocol.Response]()
      auction ! AuctionProtocol.Commands.`bid lot`(AuctionId("1"), Bid(user0, exLot1, 5), bidder.ref)
      bidder.expectMessage(LotProtocol.Responses.Successes.BidSuccessful(5, true))
    }
    "try an invalid bid" in {
      val bidder = testKit.createTestProbe[LotProtocol.Response]()
      auction ! AuctionProtocol.Commands
        .`bid lot`(AuctionId("1"), Bid(user0, exLot1, -5), bidder.ref)
      bidder.expectMessage(LotProtocol.Responses.Failures.`bid must be positive`)
    }
    "try a higher bid" in {
      val bidder = testKit.createTestProbe[LotProtocol.Response]()
      auction ! AuctionProtocol.Commands
        .`bid lot`(AuctionId("1"), Bid(user1, exLot1, 10), bidder.ref)
      bidder.expectMessage(LotProtocol.Responses.Successes.BidSuccessful(10, true))
    } /*
    "bid lower than the max of others" in {
      val bidder = testKit.createTestProbe[LotActor.BidResult]()
      auction ! AuctionActor.BidOn(exLot1, user0, 8, bidder.ref)
      bidder.expectMessage(LotActor.BidSuccessful(false))
    }*/

    "end" in {
      auction ! AuctionProtocol.Commands.End(AuctionId("1"), probe.ref)
      probe.expectMessage(AuctionProtocol.Responses.Successes.Ended(AuctionId("1")))
      testKit.stop(auction)
    }
  }
}
