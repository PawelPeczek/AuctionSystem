package AuctionSystem.Actors

import AuctionSystem.Actors.AuctionSearch.Register
import AuctionSystem.ActorsSpecifications.AuctionSpecification
import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent._
import ExecutionContext.Implicits.global

object Seller{
  def props(name: String): Props = Props(new Seller(name))

  case object AuctionRegFine
  case object AuctionRegFailure

  final case class AuctionUpdateStatus(auctName: String, iniitValue: Double, leader: ActorRef)
  final case class MakeAuction(auction: AuctionSpecification)
  final case class MakeBatchAuctions(auctions: List[AuctionSpecification])
}

class Seller(sellerNname: String) extends SystemUser {
  import SystemUser._
  import Seller._
  private var auctionCounter: Int = 0

  override def preStart(): Unit = log.info("Seller {} has started", sellerNname)

  override def postStop(): Unit = log.info("Seller {} has stopped", sellerNname)

  override def receive: Receive = {
    case MakeAuction(specs) => spawnAuction(specs)
    case MakeBatchAuctions(auctionsSpecs) => prepareBatchAuction(auctionsSpecs)
    case AuctionUpdateStatus(auctName, value, leader) =>
      log.info("Seller {}: new bid on auction {} from {}. Current value {}", sellerNname, auctName, leader.path.name, value)
    case AuctionFinalStatus(auctName, value, _, buyer) =>
      log.info("Seller {}: item from auction {} was sold to {} for {}", sellerNname, auctName, buyer.path.name, value)
      auctionCounter -= 1
      if(auctionCounter == 0) context.stop(self)
  }

  private def prepareBatchAuction(auctionsSpecs: List[AuctionSpecification]): Unit =
    auctionsSpecs.foreach(spawnAuction)


  private def spawnAuction(specs: AuctionSpecification): Unit = {
    import AuctionSearch.AuctionSearchActorName
    implicit val timeout = Timeout(500 millis)
    val auctionSearch = context.actorSelection(s"../$AuctionSearchActorName")
    val registeredAuction = context.actorOf(Auction.props(self, specs))
    val registeredStatus = auctionSearch ? Register(specs.auctName, registeredAuction)
    registeredStatus onComplete {
      case Success(resp) => dispatchResponse(resp, specs.auctName)
      case Failure(e) => log.error(e, e.getMessage)
    }

  }

  def dispatchResponse(resp: Any, auctName: String): Unit = {
    resp match {
      case AuctionRegFine =>
        log.info("Seller {} registered successfully auction {}", sellerNname, auctName)
        auctionCounter += 1
      case _ =>
        log.warning("Seller {} couldn't register successfully auction {}", sellerNname, auctName)
    }
  }

}
