package AuctionSystem.Actors

import AuctionSystem.Actors.Auction.{Bid, BidTimerExpired, DeleteTimerExpired, StartAuction}
import AuctionSystem.Actors.Buyer.{AuctionClosed, MakeBidResponse}
import AuctionSystem.Actors.SystemUser.AuctionFinalStatus
import AuctionSystem.ActorsSpecifications.AuctionSpecification
import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class AuctionSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Auction actor" must {

    "properly accept higher bid" in {
      val auctSpec1 = AuctionSpecification("Passat 1.9 TDI", 25 seconds, 5 seconds, 0)
      val seller = system.actorOf(Seller.props("marek"))
      val act = system.actorOf(Auction.props(seller ,auctSpec1))
      act ! Bid("grazyna", 2)
      expectMsgType[MakeBidResponse]
      act ! Bid("grazyna", 5)
      expectMsgType[MakeBidResponse]
      act ! PoisonPill
      seller ! PoisonPill
    }

    "properly reject lower bid" in {
      val auctSpec1 = AuctionSpecification("Passat 1.9 TDI", 25 seconds, 5 seconds, 0)
      val seller = system.actorOf(Seller.props("marek"))
      val act = system.actorOf(Auction.props(seller ,auctSpec1))
      act ! Bid("grazyna", 5)
      expectMsgType[MakeBidResponse]
      act ! Bid("grazyna", 2)
      expectMsgType[MakeBidResponse]
      act ! PoisonPill
      seller ! PoisonPill
    }

    "properly end auction" in{
      val auctSpec1 = AuctionSpecification("Passat 1.9 TDI", 2 seconds, 1 seconds, 0)
      val seller = system.actorOf(Seller.props("marek"))
      val act = system.actorOf(Auction.props(seller ,auctSpec1))
      act ! Bid("grazyna", 5)
      expectMsgType[MakeBidResponse]
      act ! BidTimerExpired
      act ! DeleteTimerExpired
      expectMsgType[MakeBidResponse]
      expectMsgType[AuctionFinalStatus]
      act ! PoisonPill
      seller ! PoisonPill
    }

    "become ignored after given period" in{
      val auctSpec1 = AuctionSpecification("Passat 1.9 TDI", 1 seconds, 1 seconds, 0)
      val probe = TestProbe()
      val act = system.actorOf(Props(new Auction(null ,auctSpec1){
        override def ignored() = {
          case _ =>  probe.ref ! "a"
        }
      }))
      within(1 seconds , 4 seconds) {
        probe.expectMsg("a")
      }
      act ! PoisonPill
    }

    "inform buyer who lost about end of auction" in {
      val auctSpec1 = AuctionSpecification("Passat 1.9 TDI", 2 seconds, 1 seconds, 0)
      val seller = system.actorOf(Seller.props("marek"))
      val act = system.actorOf(Auction.props(seller ,auctSpec1))
      act ! Bid("tester", 1)
      val bidder = system.actorOf(Props(new Buyer("maciek"){
        act ! Bid("testAct", 2)
      }))
      expectMsgType[AuctionClosed]
      act ! PoisonPill
      bidder ! PoisonPill
    }
  }
}