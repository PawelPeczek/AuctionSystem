package AuctionSystem.FSMInitializer

import AuctionSystem.Actors.AuctionSearch.STOP
import AuctionSystem.Actors.Buyer.TakePartIn
import AuctionSystem.Actors.Seller.{MakeAuction, MakeBatchAuctions}
import AuctionSystem.Actors.{AuctionSearch, Buyer, Seller}
import AuctionSystem.ActorsSpecifications.AuctionSpecification
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.collection.immutable.HashMap
import scala.concurrent.duration._
import scala.io.StdIn


class Initializer {
  def initialize(): Unit = {
    implicit val timeout = Timeout(5 seconds)
    val system = ActorSystem("AuctionSystem")
    try {
      val searchActor = system.actorOf(AuctionSearch.props(), AuctionSearch.AuctionSearchActorName)

      val seller1 = system.actorOf(Seller.props("Mirosław"), "Miroslaw")
      val seller2 = system.actorOf(Seller.props("Janusz"), "Janusz")
      val seller3 = system.actorOf(Seller.props("Grażyna"), "Grazyna")

      val auctSpec1 = AuctionSpecification("Passat 1.9 TDI", 25 seconds, 5 seconds, 20)
      val auctSpec2 = AuctionSpecification("Golf IV 1.9 TDI (Niemiec do granicy gonił)", 15 seconds, 5 seconds, 13)
      val auctSpec3 = AuctionSpecification("Sprzedam somsiada!", 35 seconds, 5 seconds, 2)
      val auctSpec4 = AuctionSpecification("Sprzedam psa", 30 seconds, 5 seconds, 5)

      val search1: HashMap[String, Double] = HashMap(("Passat", 35), ("1.9", 27))
      val search2: HashMap[String, Double] = HashMap(("psa", 8.5), ("somsiada", 10))
      val search3: HashMap[String, Double] = HashMap(("Golf", 35), ("sprzedam", 9.5))

      val buyer1 = system.actorOf(Buyer.props("Pjoter"))
      val buyer2 = system.actorOf(Buyer.props("Dżesika"))
      val buyer3 = system.actorOf(Buyer.props("Brajan"))

      seller1 ! MakeBatchAuctions(List(auctSpec1, auctSpec2))
      seller2 ! MakeAuction(auctSpec3)
      seller3 ! MakeAuction(auctSpec4)

      StdIn.readLine()
      println("[ENTER PRESSED]")
      buyer1 ! TakePartIn(search1)
      buyer2 ! TakePartIn(search2)
      buyer3 ! TakePartIn(search3)

      StdIn.readLine()
      println("[ENTER PRESSED]")
      searchActor ! STOP
    } finally {
      system.terminate()
    }
  }
}
