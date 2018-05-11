package AuctionSystem.Messages

import akka.actor.ActorRef

final case class AuctionStatus(name: String, value: Double, seller: ActorRef, buyer: ActorRef)