package AuctionSystem.Messages

import akka.actor.ActorRef

final case class Bid(value: Double, buyer: ActorRef)
