package AuctionSystem.Messages

import akka.actor.ActorRef

final case class AuctionUpdateStatus(auctName: String, value: Double, leader: ActorRef)
