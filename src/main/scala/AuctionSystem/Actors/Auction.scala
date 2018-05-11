package AuctionSystem.Actors

import AuctionSystem.Messages._
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}

import scala.concurrent.duration._


class Auction extends  Actor with ActorLogging {
  import context.dispatcher
  private var name : String = _
  private var bidTimeout : FiniteDuration = Duration.Zero
  private var deleteTimeout : FiniteDuration = Duration.Zero
  private var cancelBidTimeout: Cancellable = Cancellable.alreadyCancelled
  private var cancellDeleteTimeout: Cancellable = Cancellable.alreadyCancelled
  private var bidValue : Double = 0
  private var seller : ActorRef = ActorRef.noSender
  private var buyer : ActorRef = ActorRef.noSender

  override def receive: Receive = {
    case StartAuction(_bidTimeout, _deleteTimeout, _name) =>
      name = _name
      bidTimeout = _bidTimeout
      deleteTimeout = _deleteTimeout
      log.info("Started auction {} with timeout {}s", _name, bidTimeout.toSeconds)
      setCancelBidTimeout()
      created()
  }

  def created(): Receive = {
    case BidTimerExpired =>
      log.info("Auction {} exceeded bidTimeout", name)
      setDeleteBidTimeout()
      ignored()
    case Bid(_value) => activate_loop(_value)
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
    case Bid(_value) => activate_loop(_value)
  }

  def activate_loop(_value: Double): Unit ={
    if (_value > bidValue) {
      log.info("Auction {} got valid bid", name)
      bidValue = _value
      cancelBidTimeout.cancel()
      setCancelBidTimeout()
      activated()
    }
  }

  def sold(): Receive = {
    case DeleteTimerExpired =>
      log.info("Item {} is deleting", name)
  }

  def notify_parties(): Unit ={

  }

  private def setCancelBidTimeout() : Unit = {
    cancelBidTimeout = context.system.scheduler.scheduleOnce(
      bidTimeout,
      self,
      BidTimerExpired
    )
  }

  private def setDeleteBidTimeout(): Unit = {
    cancellDeleteTimeout = context.system.scheduler.scheduleOnce(
      bidTimeout,
      self,
      DeleteTimerExpired
    )
  }
}
