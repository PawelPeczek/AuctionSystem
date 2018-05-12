package AuctionSystem.Actors

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent._
import ExecutionContext.Implicits.global

object Buyer {
  def props(buyName: String) : Props = Props(new Buyer(buyName))

  class MakeBidStatus()
  case object OK extends MakeBidStatus
  case object FAILED extends MakeBidStatus
  case object LOST_LEADERSHIP extends MakeBidStatus

  case object SearchFiled

  final case class SearchResult(auctions: List[ActorRef])

  final case class MakeBidResponse(status: MakeBidStatus, name: String, currLeadingVal: Double)
  final case class TakePartIn(keywordsAndLimits: HashMap[String, Double])

}

class Buyer(buyName: String) extends SystemUser {
  import Buyer._
  import SystemUser._
  import Auction.Bid

  val epsilon: Double = 0.5
  val currentBids: mutable.HashMap[ActorRef, (Double, Double)] = mutable.HashMap()

  override def preStart(): Unit = log.info("Buyer {} has started", buyName)

  override def postStop(): Unit = log.info("Buyer {} has stopped", buyName)


  override def receive: Receive = {
    case TakePartIn(keywordsAndLimits) => launchBids(keywordsAndLimits)
    case MakeBidResponse(OK, actName, currLeadingVal) =>
      log.info("Buyer {} is leading in auction {} (current leading value: {})",
              buyName, actName, currLeadingVal)
    case MakeBidResponse(FAILED, actName, currLeadingVal) =>
      log.info("Buyer {} failed to take leadership in auction {} (current leading value: {})",
              buyName, actName, currLeadingVal)
      tryToTakeLeadership(actName, currLeadingVal)
    case MakeBidResponse(LOST_LEADERSHIP, actName, currLeadingVal) =>
      log.info("Buyer {} lost leadership in auction {} (current leading value: {})",
              buyName, actName, currLeadingVal)
      tryToTakeLeadership(actName, currLeadingVal)
    case AuctionFinalStatus(actName, bidValue, _, _) =>
      log.info("Buyer {} won the auction {} with value {}", buyName, actName, bidValue)
  }

  private def launchBids(keywordsAndLimits: HashMap[String, Double]): Unit = {
    val foundAuctions = findAllAuctions(keywordsAndLimits)
    val auctToStart = foundAuctions.filter(e => !currentBids.keySet.contains(e._1))
    auctToStart.foreach(e => currentBids.put(e._1, (0, e._2)))
    auctToStart.foreach(e => {
      e._1 ! Bid(buyName, currentBids(e._1)._1)
      log.info("Buyer {} sent bid to auction {}", buyName, e._1.path.name)
    })
  }


  private def findAllAuctions(keywordsAndLimits: HashMap[String, Double]): mutable.HashMap[ActorRef, Double] = {
    import AuctionSearch.{AuctionSearchActorName, Find}
    val auctionSearch = context.actorSelection(s"../$AuctionSearchActorName")
    val allFound : mutable.HashMap[ActorRef, Double]  = new mutable.HashMap()
    implicit val timeout = Timeout(500 millis)
    keywordsAndLimits.foreach {
      keyAndLimit => {
        val response = auctionSearch ? Find(keyAndLimit._1)
        response onComplete {
          case Success(resp) => dispatchResponse(resp, allFound, keyAndLimit)
          case Failure(e) => log.error(e, e.getMessage)
        }
      }
    }

    allFound
  }

  private def dispatchResponse(response: Any, allFound: mutable.HashMap[ActorRef, Double], keyToLim: (String, Double)): Unit = {
    import Buyer.SearchResult
    response match {
      case SearchResult(auct) => auct.foreach(e => allFound.put(e, keyToLim._2))
      case _ => log.info("Buyer {} couldn't find any auction for keyword {}", buyName, keyToLim._1)
    }
  }


  private def tryToTakeLeadership(aucName: String, currLeadingVal: Double) : Unit = {
    if(canOfferMore(currLeadingVal)){
      updateCurrBid(currLeadingVal)
      sender() ! Bid(buyName, currentBids(sender())._1)
      log.info("Buyer with name {} try to retrieve leadership in auction {} with bid: {}",
              buyName, aucName, currentBids(sender())._1)
    } else {
      log.info("Buyer with name {} cannot longer take part in in auction {}", buyName, aucName)
    }
  }

  def canOfferMore(currLeadingVal: Double): Boolean = {
    currLeadingVal + epsilon <= currentBids(sender())._2
  }

  def updateCurrBid(currLeadingVal: Double): Unit = {
    currentBids(sender()) = (currLeadingVal + epsilon, currentBids(sender())._2)
  }
}
