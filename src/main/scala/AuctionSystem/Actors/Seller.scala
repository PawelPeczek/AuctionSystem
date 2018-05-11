package AuctionSystem.Actors

import AuctionSystem.Messages.{AuctionFinalStatus, AuctionUpdateStatus, MakeAuction}
import akka.actor.{Actor, ActorLogging, Props}

object Seller{
  def props(name: String): Props = Props(new Seller(name))
}

class Seller(name: String) extends Actor with ActorLogging  {
  private var auctionCounter: Int = 0

  override def preStart(): Unit = log.info("Seller {} has started", name)

  override def postStop(): Unit = log.info("Seller {} has stopped", name)

  override def receive: Receive = {
    case MakeAuction(auctName, auctPathName,  bidTimeout, deleteTimeout, bidValue) =>
      val auction = context.actorOf(Auction.props(auctName, bidTimeout, deleteTimeout, self, bidValue), auctPathName)
      sender() ! auction
      auctionCounter += 1
    case AuctionUpdateStatus(auctName, value, leader) =>
      log.info("Seller {}: new bid on auction {} from {}. Current value {}", name, auctName, value, leader.path.name)
    case AuctionFinalStatus(auctName, value, _, buyer) =>
      log.info("Seller {}: item from auction {} was sold to {} for {}", name, auctName, buyer.path.name, value)
      auctionCounter -= 1
      if(auctionCounter == 0) context.stop(self)
  }
}
