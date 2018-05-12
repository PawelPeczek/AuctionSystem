package AuctionSystem.Actors

import akka.actor.{Actor, ActorLogging, ActorRef}


object SystemUser {
  final case class AuctionFinalStatus(name: String, value: Double, seller: ActorRef, buyer: ActorRef)
}

abstract class SystemUser extends Actor with ActorLogging
