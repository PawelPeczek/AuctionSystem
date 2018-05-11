package AuctionSystem.Messages

import akka.actor.ActorRef

final case class AuctionFinalStatus(name: String, value: Double, seller: ActorRef, buyer: ActorRef)