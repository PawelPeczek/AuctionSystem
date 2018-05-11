package AuctionSystem.Actors

import AuctionSystem.Messages.{CounterResponse, GetCounter}
import akka.actor.{Actor, Props}

object BaseActor {
  def props(): Props = Props(new BaseActor)
}

class BaseActor extends Actor {

  private var requestId : Long = 1

  override def receive: Receive = {
    case "send" => {
      println("Sending request!")
      context.actorSelection("/user/AuctionSystemSupervisor/*") ! GetCounter(requestId)
      requestId += 1
      self ! "send"
    }
    case "stop" => context.stop(self)
    case CounterResponse(requestId, value) => {
      println(s"ID: $requestId - VAL: $value")
    }
  }

  override def preStart(): Unit = println("BaseActor has started!")

  override def postStop(): Unit = println("BaseActor has stopped!")
}
