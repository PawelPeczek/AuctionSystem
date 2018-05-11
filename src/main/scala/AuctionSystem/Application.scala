package AuctionSystem

import akka.actor.ActorSystem

object Application {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("AuctionSystem")
  }
}
