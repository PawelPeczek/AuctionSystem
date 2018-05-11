package AuctionSystem.Actors

import AuctionSystem.Messages.StartServer
import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import scala.concurrent.duration._

object AuctionSystemSupervisor {
  def props(): Props = Props(new AuctionSystemSupervisor)
}

class AuctionSystemSupervisor extends Actor with ActorLogging {


//  override val supervisorStrategy =
//    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
//      case _:Exception => Restart
//    }
//
  override def receive: Receive = {
    case StartServer =>
  }


  override def preStart(): Unit = log.info("AuctionSystem has started.")

  override def postStop(): Unit = log.info("AuctionSystem has stopped.")

  override def postRestart(reason: Throwable): Unit =
    log.info(s"AuctionSystem has restart after error: ${reason.getMessage}")

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    val info_message = new StringBuilder
    info_message.append("AuctionSystem error!\nError message: ")
                .append(reason.getMessage)
                .append("\nwhile processing message: ")
                .append(message.getOrElse("unknown"))
    log.error(reason, info_message.toString)
  }

}
