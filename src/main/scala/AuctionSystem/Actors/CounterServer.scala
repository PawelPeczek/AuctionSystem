package AuctionSystem.Actors

import AuctionSystem.Messages.{CounterResponse, GetCounter}
import akka.actor.{Actor, ActorLogging, Props}

object CounterServer {
  def props(serverId: Int, serverName: String) = Props(new CounterServer(serverId, serverName))
}

class CounterServer(val serverId: Int, val serverName: String) extends Actor with ActorLogging {
  private var ctr: Long = 1

  override def receive: Receive = {
    case GetCounter(id) => {
      ctr += 1
      sender() ! CounterResponse(id, ctr)
    }
  }

  override def preStart(): Unit = log.info("CounterServer {}[{}] has started", serverId, serverName)

  override def postStop(): Unit = log.info("CounterServer {}[{}] has stopped", serverId, serverName)
}
