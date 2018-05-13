package AuctionSystem.Actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.immutable.HashMap
import scala.collection.mutable

object AuctionSearch {
  def props(): Props = Props(new AuctionSearch())
  val AuctionSearchActorName : String = "AuctionSearchActor"
  case object STOP
  final case class Register(auctionName: String, auctionActor: ActorRef)
  final case class Find(keyword: String)
}

class AuctionSearch extends Actor with ActorLogging {
  import AuctionSearch._
  var namesToRef: HashMap[String, ActorRef] = HashMap()

  override def preStart(): Unit = log.info("AuctionSearch actor {} has started", self.path.name)

  override def postStop(): Unit = log.info("AuctionSearch actor {} has stopped", self.path.name)

  override def receive: Receive = {
    case STOP => context.stop(self)
    case Register(auctionName, auctionActor) => registerAuction(auctionName, auctionActor)
    case Find(keyword) => findAuctionByKeyword(keyword)
  }

  private def registerAuction(auctionName: String, auctionActor: ActorRef) = {
    import Seller._
    log.info("AuctionSearch actor {} got registration request of auction {}", self.path.name, auctionName)
    if (!namesToRef.contains(auctionName.toLowerCase)) {
      namesToRef += auctionName.toLowerCase -> auctionActor
      sender() ! AuctionRegFine
      log.info("AuctionSearch actor {} registered auction {}", self.path.name, auctionName)
    } else {
      sender() ! AuctionRegFailure
      log.info("AuctionSearch actor {} couldn't register auction {}", self.path.name, auctionName)
    }
  }

  private def findAuctionByKeyword(keyword: String): Unit = {
    import Buyer.{SearchFiled, SearchResult}
    log.info("AuctionSearch actor {} got find request with keyword {}", self.path.name, keyword)
    if(isValid(keyword)){
      provideAuctionsActorsByKeyword(keyword) match {
        case None =>
          log.info("AuctionSearch actor {} couldn't find auctions matching to {}", self.path.name, keyword)
          sender() ! SearchFiled
        case Some(auctionList) =>
          log.info("AuctionSearch actor {} found auctions matching to {}", self.path.name, keyword)
          sender() ! SearchResult(auctionList)
      }
    } else {
      sender() ! SearchFiled
    }
  }

  private def isValid(keyword: String): Boolean = !keyword.contains(" ")

  private def provideAuctionsActorsByKeyword(keyword: String): Option[List[ActorRef]] = {
    val keysToReturn = namesToRef.keySet.filter(_.contains(keyword.toLowerCase))
    if(keysToReturn.isEmpty)
      None
    else
      Some(namesToRef.filter(e => keysToReturn.contains(e._1)).values.toList)
  }
}
