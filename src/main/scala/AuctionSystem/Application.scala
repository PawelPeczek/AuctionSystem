package AuctionSystem

import AuctionSystem.Actors.{AuctionSystemSupervisor, BaseActor}
import AuctionSystem.Messages.StartServer
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.io.StdIn

object Application {
  def main(args: Array[String]): Unit = {
    implicit val timeout = Timeout(5 seconds)
    val system = ActorSystem("AuctionSystem")
    try {
      val supervisor = system.actorOf(AuctionSystemSupervisor.props(), "AuctionSystemSupervisor")
      supervisor ? StartServer
      val child1 = system.actorOf(BaseActor.props(), "child-1")
      val child2 = system.actorOf(BaseActor.props(), "child-2")
      child1 ! "send"
      child2 ! "send"
      StdIn.readLine()
    } finally {
      system.terminate()
    }
  }
}
