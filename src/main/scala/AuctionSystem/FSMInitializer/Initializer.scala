package AuctionSystem.FSMInitializer

import AuctionSystem.Actors.{Buyer, Seller}
import AuctionSystem.Messages.{MakeAuction, StartAuction, StartBid}
import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.pattern.ask

import scala.util.{Failure, Success}
import scala.collection.immutable.HashMap
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn


class Initializer {
  def initialize(): Unit = {
    implicit val timeout = Timeout(5 seconds)
    val system = ActorSystem("AuctionSystem")
    try {
      val seller1 = system.actorOf(Seller.props("Mirosław"), "Miroslaw")
      val seller2 = system.actorOf(Seller.props("Janusz"), "Janusz")
      val seller3 = system.actorOf(Seller.props("Grażyna"), "Grazyna")
      val auct1Name = "Passat 1.9 TDI"
      val auct2Name = "Golf IV (Niemiec do granicy gonił)"
      val auct3Name = "Sprzedam somsiada!"
      val auct4Name = "Sprzedam psa!"
      val shortA1Name = "a1"
      val shortA2Name = "a2"
      val shortA3Name = "a3"
      val shortA4Name = "a4"
      val fauct1 = seller1 ? MakeAuction(auct1Name, shortA1Name, 1 minute, 15 seconds, 20)
      val fauct2 = seller1 ? MakeAuction(auct2Name, shortA2Name, 30 seconds, 15 seconds, 13)
      val fauct3 = seller2 ? MakeAuction(auct3Name, shortA3Name, 40 seconds, 10 seconds, 2)
      val fauct4 = seller3 ? MakeAuction(auct4Name, shortA4Name, 35 seconds, 15 seconds, 5)
      var auct1 : ActorRef = Await.result(fauct1, 1 minute).asInstanceOf[ActorRef]
      var auct2 : ActorRef = Await.result(fauct2, 1 minute).asInstanceOf[ActorRef]
      var auct3 : ActorRef = Await.result(fauct3, 1 minute).asInstanceOf[ActorRef]
      var auct4 : ActorRef = Await.result(fauct4, 1 minute).asInstanceOf[ActorRef]
      val list1 = List(auct1, auct2, auct4)
      val limits1: HashMap[String, Double] = HashMap((shortA1Name, 35), (shortA2Name, 20), (shortA4Name, 7.55))
      val list2 = List(auct2, auct3, auct4)
      val limits2: HashMap[String, Double] = HashMap((shortA2Name, 23), (shortA3Name, 2.75), (shortA4Name, 6.95))
      val list3 = List(auct1, auct3, auct4)
      val limits3: HashMap[String, Double] = HashMap((shortA1Name, 36), (shortA3Name, 3), (shortA4Name, 10.55))
      val buyer1 = system.actorOf(Buyer.props("Pjoter", list1, limits1), "Pjoter")
      val buyer2 = system.actorOf(Buyer.props("Dżesika", list2, limits2), "Dzesika")
      val buyer3 = system.actorOf(Buyer.props("Brajan", list3, limits3), "Brajan")
      auct1 ! StartAuction
      auct2 ! StartAuction
      auct3 ! StartAuction
      auct4 ! StartAuction
      buyer1 ! StartBid
      buyer2 ! StartBid
      buyer3 ! StartBid
      StdIn.readLine()
    } finally {
      system.terminate()
    }
  }
}
