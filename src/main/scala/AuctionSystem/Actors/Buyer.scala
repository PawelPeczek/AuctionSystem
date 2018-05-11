package AuctionSystem.Actors

import AuctionSystem.Messages._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.immutable.HashMap
import scala.collection.mutable

object Buyer {
  def props(buyName: String, auctions: List[ActorRef], limits: HashMap[String, Double]) : Props =
    Props(new Buyer(buyName, auctions, limits))
}

class Buyer(buyName: String, auctions: List[ActorRef], limits: HashMap[String, Double]) extends Actor with ActorLogging{
  val epsilon: Double = 0.5
  val currentBids: mutable.HashMap[String, Double] = mutable.HashMap()

  override def receive: Receive = {
    case "start" =>
      initializeBids()
      
    case MakeBidResponse(OK, actName) =>
      log.info("Buyer with actName {} is leading in auction {}", buyName, actName)
    case MakeBidResponse(FAILED, actName) =>
      log.info("Buyer with actName {} failed to take leadership in auction {}", buyName, actName)
      tryToTakeLeadership(actName)
    case MakeBidResponse(LOST_LEADERSHIP, actName) =>
      log.info("Buyer with actName {} lost leadership in auction {}", buyName, actName)
      tryToTakeLeadership(actName)
    case AuctionStatus(actName, bidValue, seller, buyer) =>
      log.info("Buyer with name {} won the auction {} with value {}", buyName, actName, bidValue)
  }

  def initializeBids(): Unit = {
    for(limit <- limits) {
      if(epsilon < limit._2) currentBids.put(limit._1, epsilon)
      else  currentBids.put(limit._1, 0)
    }
  }
  
  def startBids() : Unit = {
    var it: Int = 0
    for(auction <- auctions){
      auction ! Bid(currentBids(auction.path.name), self)
      it += 1
    }
  }

  def tryToTakeLeadership(aucName: String) : Unit = {
    val auctionToBid : ActorRef = auctions.filter(_.path.name == aucName).head
    if(canOfferMore(auctionToBid.path.name)){
      updateCurrBid(auctionToBid.path.name)
      auctionToBid ! Bid(currentBids(auctionToBid.path.name), self)
      log.info("Buyer with name {} try to retrieve leadership in auction {}", buyName, aucName)
    } else {
      log.info("Buyer with name {} cannot longer take part in in auction {}", buyName, aucName)
    }
  }

  def canOfferMore(name: String): Boolean = {
    currentBids(name) + epsilon <= limits(name)
  }

  def updateCurrBid(name: String): Unit = {
    currentBids(name) = currentBids(name) + epsilon
  }
}
