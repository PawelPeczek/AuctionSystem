package AuctionSystem

import AuctionSystem.FSMInitializer.Initializer

object Application {
  def main(args: Array[String]): Unit = {
    val init = new Initializer
    init.initialize()
  }
}
