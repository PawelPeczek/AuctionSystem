package AuctionSystem.Actors

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}

import scala.concurrent.duration._

object Auction {
  def props(name : String, bidTimeout : FiniteDuration, deleteTimeout : FiniteDuration,
            seller : ActorRef, bidValue: Double = 0): Props =
    Props(new Auction(name, bidTimeout, deleteTimeout, seller, bidValue))

  case object DeleteTimerExpired
  case object BidTimerExpired
  case object ReList
  case object StartAuction

  final case class Bid(value: Double)

}

class Auction(name : String, bidTimeout : FiniteDuration, deleteTimeout : FiniteDuration,
              seller : ActorRef, var bidValue: Double = 0) extends Actor with ActorLogging {
  import context.dispatcher
  import Auction._
  private var cancelBidTimeout: Cancellable = Cancellable.alreadyCancelled
  private var cancelDeleteTimeout: Cancellable = Cancellable.alreadyCancelled
  private var buyer : ActorRef = ActorRef.noSender

  override def postStop(): Unit = log.info("Auction {} has stopped", name)

  override def receive: Receive = {
    case StartAuction =>
      log.info("Started auction {} with timeout {}s", name, bidTimeout.toSeconds)
      setCancelBidTimeout()
      log.info("Auction {} become CREATED", name)
      context become created
  }

  def created(): Receive = {
    case BidTimerExpired =>
      log.info("Auction {} exceeded bidTimeout", name)
      setDeleteBidTimeout()
      log.info("Auction {} become IGNORED", name)
      context become ignored
    case Bid(_value) =>
      log.info("Auction {} received bid from {}. Value: {}", name, sender().path.name, _value)
      activate_loop(_value)
  }

  def ignored(): Receive = {
    case DeleteTimerExpired =>
      log.info("Auction {} exceeded deleteTimeout and is closing", name)
      context.stop(self)
    case ReList =>
      cancelDeleteTimeout.cancel()
      log.info("Auction {} got relist message", name)
      setCancelBidTimeout()
      log.info("Auction {} become CREATED", name)
      context become created
  }

  def activated(): Receive = {
    case BidTimerExpired =>
      setDeleteBidTimeout()
      log.info("Auction {} become SOLD", name)
      context become sold
    case Bid(_value) => activate_loop(_value)
  }

  def activate_loop(_value: Double): Unit = {
    import Buyer._
    import Seller._
    if (_value > bidValue) {
      cancelBidTimeout.cancel()
      log.info("Auction {} got valid bid from {}. Current leading value: {}", name, sender().path.name, _value)
      bidValue = _value
      sender() ! MakeBidResponse(OK, self.path.name, bidValue)
      if(buyer != ActorRef.noSender) buyer ! MakeBidResponse(LOST_LEADERSHIP, self.path.name, bidValue)
      seller ! AuctionUpdateStatus(name, bidValue, sender())
      buyer = sender()
      setCancelBidTimeout()
    } else {
      log.info("Auction {} got invalid bid from {}. Current leading value: {}", name, sender().path.name, bidValue)
      sender() !  MakeBidResponse(FAILED, self.path.name, bidValue)
    }
    log.info("Auction {} become ACTIVATED", name)
    context become activated
  }

  def sold(): Receive = {
    case DeleteTimerExpired =>
      notify_parties()
      log.info("Item {} is deleting", name)
      context.stop(self)
  }

  def notify_parties(): Unit ={
    import SystemUser._
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
