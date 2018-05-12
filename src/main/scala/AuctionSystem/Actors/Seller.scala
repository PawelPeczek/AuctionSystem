package AuctionSystem.Actors

import akka.actor.{ActorRef, Props}

import scala.concurrent.duration.FiniteDuration

object Seller{
  def props(name: String): Props = Props(new Seller(name))
  final case class AuctionUpdateStatus(auctName: String, value: Double, leader: ActorRef)
  final case class MakeAuction(auctName: String, auctPathName: String,  bidTimeout : FiniteDuration,
                               deleteTimeout : FiniteDuration, bidValue: Double = 0)
}

class Seller(name: String) extends SystemUser {
  import SystemUser._
  import Seller._
  private var auctionCounter: Int = 0

  override def preStart(): Unit = log.info("Seller {} has started", name)

  override def postStop(): Unit = log.info("Seller {} has stopped", name)

  override def receive: Receive = {
    case MakeAuction(auctName, auctPathName,  bidTimeout, deleteTimeout, bidValue) =>
      val auction = context.actorOf(Auction.props(auctName, bidTimeout, deleteTimeout, self, bidValue), auctPathName)
      sender() ! auction
      auctionCounter += 1
    case AuctionUpdateStatus(auctName, value, leader) =>
      log.info("Seller {}: new bid on auction {} from {}. Current value {}", name, auctName, leader.path.name, value)
    case AuctionFinalStatus(auctName, value, _, buyer) =>
      log.info("Seller {}: item from auction {} was sold to {} for {}", name, auctName, buyer.path.name, value)
      auctionCounter -= 1
      if(auctionCounter == 0) context.stop(self)
  }
}
