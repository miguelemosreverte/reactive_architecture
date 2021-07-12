import akka.actor.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Routers
import akka.routing.Routee
import org.scalatest.flatspec.AnyFlatSpec

import collection.mutable.Stack
import org.scalatest._
import flatspec._
import io.scalac.auction.lot.Domain.{Bid, Lot, LotId, User, UserId}
import io.scalac.auction.lot.Protocol.Responses.Successes.BidSuccessful
import io.scalac.auction.lot.{Domain, Logic, Protocol}
import matchers._

class RouteTest extends AnyFlatSpec with should.Matchers {

  object Dataset {
    object Users {
      val `user A` = User(UserId("A"))
      val `user B` = User(UserId("B"))
      val `user C` = User(UserId("C"))
    }
    object Lots {
      val `lot A` = Lot(LotId("A"))
    }
  }
  import Dataset.Users._
  import Dataset.Lots._

  val bids = Set(
    Bid(`user A`, `lot A`, 1),
    Bid(`user B`, `lot A`, 1)
  )
  val dataset = Logic(bids, None) _
  "lot" should "pepe" in {
    dataset(Protocol.Commands.Bid(Bid(`user B`, `lot A`, 3), null)) should be(
      Right(BidSuccessful(3, true))
    )
  }
}
