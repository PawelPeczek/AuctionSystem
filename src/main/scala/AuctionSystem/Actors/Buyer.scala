package AuctionSystem.Actors

import akka.actor.{ActorRef, Props}

import scala.collection.immutable.HashMap
import scala.collection.mutable

object Buyer {
  def props(buyName: String, auctions: List[ActorRef], limits: HashMap[String, Double]) : Props =
    Props(new Buyer(buyName, auctions, limits))

  class MakeBidStatus()

  case object StartBid
  case object OK extends MakeBidStatus
  case object FAILED extends MakeBidStatus
  case object LOST_LEADERSHIP extends MakeBidStatus

  final case class MakeBidResponse(status: MakeBidStatus, name: String, currLeadingVal: Double)

}

class Buyer(buyName: String, auctions: List[ActorRef], limits: HashMap[String, Double]) extends SystemUser {
  import Buyer._
  import SystemUser._
  import Auction.Bid
  val epsilon: Double = 0.5
  val currentBids: mutable.HashMap[String, Double] = mutable.HashMap()
  override def preStart(): Unit = log.info("Buyer {} has started", buyName)

  override def postStop(): Unit = log.info("Buyer {} has stopped", buyName)

  override def receive: Receive = {
    case StartBid =>
      log.info("Buyer {} is starting bids!", buyName)
      initializeBids()
      startBids()
    case MakeBidResponse(OK, actShortName, currLeadingVal) =>
      log.info("Buyer with actName {} is leading in auction {} (current leading value: {})",
              buyName, actShortName, currLeadingVal)
    case MakeBidResponse(FAILED, actShortName, currLeadingVal) =>
      log.info("Buyer with actName {} failed to take leadership in auction {} (current leading value: {})",
              buyName, actShortName, currLeadingVal)
      tryToTakeLeadership(actShortName, currLeadingVal)
    case MakeBidResponse(LOST_LEADERSHIP, actShortName, currLeadingVal) =>
      log.info("Buyer with actName {} lost leadership in auction {} (current leading value: {})",
              buyName, actShortName, currLeadingVal)
      tryToTakeLeadership(actShortName, currLeadingVal)
    case AuctionFinalStatus(actName, bidValue, _, _) =>
      log.info("Buyer with name {} won the auction {} with value {}", buyName, actName, bidValue)
  }

  def initializeBids(): Unit = {
    for(limit <- limits) {
      if(epsilon < limit._2) currentBids.put(limit._1, epsilon)
      else  currentBids.put(limit._1, 0)
    }
  }
  
  def startBids() : Unit = {
    for(auction <- auctions){
      auction ! Bid(currentBids(auction.path.name))
      log.info("Buyer {} sent bid to auction {}", buyName, auction.path.name)
    }
  }

  def tryToTakeLeadership(aucName: String, currLeadingVal: Double) : Unit = {
    val auctionToBid : ActorRef = auctions.filter(_.path.name == aucName).head
    if(canOfferMore(auctionToBid.path.name, currLeadingVal)){
      updateCurrBid(auctionToBid.path.name, currLeadingVal)
      auctionToBid ! Bid(currentBids(auctionToBid.path.name))
      log.info("Buyer with name {} try to retrieve leadership in auction {} with bid: {}",
              buyName, aucName, currentBids(auctionToBid.path.name))
    } else {
      log.info("Buyer with name {} cannot longer take part in in auction {}", buyName, aucName)
    }
  }

  def canOfferMore(name: String, currLeadingVal: Double): Boolean = {
    currLeadingVal + epsilon <= limits(name)
  }

  def updateCurrBid(name: String, currLeadingVal: Double): Unit = {
    currentBids(name) = currLeadingVal + epsilon
  }
}
