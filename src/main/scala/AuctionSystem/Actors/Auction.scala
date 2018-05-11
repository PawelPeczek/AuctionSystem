package AuctionSystem.Actors

import AuctionSystem.Messages._
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}

import scala.concurrent.duration._

object Auction {
  def props(name : String, bidTimeout : FiniteDuration, deleteTimeout : FiniteDuration,
            seller : ActorRef, bidValue: Double = 0): Props =
    Props(new Auction(name, bidTimeout, deleteTimeout, seller, bidValue))
}

class Auction(name : String, bidTimeout : FiniteDuration, deleteTimeout : FiniteDuration,
              seller : ActorRef, var bidValue: Double = 0) extends Actor with ActorLogging {
  import context.dispatcher
  private var cancelBidTimeout: Cancellable = Cancellable.alreadyCancelled
  private var cancelDeleteTimeout: Cancellable = Cancellable.alreadyCancelled
  private var buyer : ActorRef = ActorRef.noSender

  override def postStop(): Unit = log.info("Auction {} has stopped", name)

  override def receive: Receive = {
    case StartAuction =>
      log.info("Started auction {} with timeout {}s", name, bidTimeout.toSeconds)
      seller ! "ok"
      setCancelBidTimeout()
      created()
  }

  def created(): Receive = {
    case BidTimerExpired =>
      log.info("Auction {} exceeded bidTimeout", name)
      setDeleteBidTimeout()
      ignored()
    case Bid(_value, _buyer) =>
      log.info("Auction {} received bid from {}. Value: {}", name, _buyer.path.name, _value)
      activate_loop(_value, _buyer)
  }

  def ignored(): Receive = {
    case DeleteTimerExpired =>
      log.info("Auction {} exceeded deleteTimeout and is closing", name)
      context.stop(self)
    case Relist =>
      cancelBidTimeout.cancel()
      log.info("Auction {} got relist message", name)
  }

  def activated(): Receive = {
    case BidTimerExpired =>
      setDeleteBidTimeout()
      notify_parties()
      sold()
    case Bid(_value, _buyer) => activate_loop(_value, _buyer)
  }

  def activate_loop(_value: Double, _buyer: ActorRef): Unit = {
    if (_value > bidValue) {
      cancelBidTimeout.cancel()
      log.info("Auction {} got valid bid", name)
      bidValue = _value
      _buyer ! MakeBidResponse(OK, name)
      buyer ! MakeBidResponse(LOST_LEADERSHIP, name)
      seller ! AuctionUpdateStatus(name, bidValue, _buyer)
      buyer = _buyer
      setCancelBidTimeout()
    } else {
      _buyer !  MakeBidResponse(FAILED, name)
    }
    activated()
  }

  def sold(): Receive = {
    case DeleteTimerExpired =>
      log.info("Item {} is deleting", name)
      context.stop(self)
  }

  def notify_parties(): Unit ={
    val status = AuctionFinalStatus(name, bidValue, seller, buyer)
    seller ! status
    buyer ! status
  }

  private def setCancelBidTimeout() : Unit = {
    cancelBidTimeout = context.system.scheduler.scheduleOnce(
      bidTimeout,
      self,
      BidTimerExpired
    )
  }

  private def setDeleteBidTimeout(): Unit = {
    cancelDeleteTimeout = context.system.scheduler.scheduleOnce(
      bidTimeout,
      self,
      DeleteTimerExpired
    )
  }
}
