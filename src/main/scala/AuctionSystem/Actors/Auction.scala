package AuctionSystem.Actors

import AuctionSystem.Actors.Buyer.AuctionClosed
import AuctionSystem.ActorsSpecifications.AuctionSpecification
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}


object Auction {
  def props(seller : ActorRef, specs: AuctionSpecification): Props =
    Props(new Auction(seller, specs))

  case object DeleteTimerExpired
  case object BidTimerExpired
  case object ReList
  case object StartAuction

  final case class Bid(name: String, value: Double)

}

class Auction(seller: ActorRef, specs: AuctionSpecification) extends Actor with ActorLogging {
  import context.dispatcher
  import Auction._
  private var bidValue = specs.bidInitValue
  private var cancelBidTimeout: Cancellable = Cancellable.alreadyCancelled
  private var cancelDeleteTimeout: Cancellable = Cancellable.alreadyCancelled
  private var buyer : ActorRef = ActorRef.noSender
  private var allBuyersThatMadeBid: Set[ActorRef] = Set()

  override def postStop(): Unit = log.info("Auction {} has stopped", specs.auctName)

  override def receive: Receive = {
    case StartAuction =>
      log.info("Started auction {} with timeout {}s", specs.auctName, specs.bidTimeout.toSeconds)
      setCancelBidTimeout()
      log.info("Auction {} become CREATED", specs.auctName)
      context become created
  }

  def created(): Receive = {
    case BidTimerExpired =>
      log.info("Auction {} exceeded bidTimeout", specs.auctName)
      setDeleteBidTimeout()
      log.info("Auction {} become IGNORED", specs.auctName)
      context become ignored
    case Bid(_name, _value) =>
      log.info("Auction {} received bid from {}. Value: {}", specs.auctName, _name, _value)
      activate_loop(_name, _value)
  }

  def ignored(): Receive = {
    case DeleteTimerExpired =>
      log.info("Auction {} exceeded deleteTimeout and is closing", specs.auctName)
      context.stop(self)
    case ReList =>
      cancelDeleteTimeout.cancel()
      log.info("Auction {} got relist message", specs.auctName)
      setCancelBidTimeout()
      log.info("Auction {} become CREATED", specs.auctName)
      context become created
  }

  def activated(): Receive = {
    case BidTimerExpired =>
      setDeleteBidTimeout()
      log.info("Auction {} become SOLD", specs.auctName)
      context become sold
    case Bid(_name, _value) => activate_loop(_name, _value)
  }

  def activate_loop(_name: String, _value: Double): Unit = {
    import Buyer._
    import Seller._
    if (_value > bidValue) {
      cancelBidTimeout.cancel()
      log.info("Auction {} got valid bid from {}. Current leading value: {}", specs.auctName, _name, _value)
      bidValue = _value
      sender() ! MakeBidResponse(OK, specs.auctName, bidValue)
      if(buyer != ActorRef.noSender) buyer ! MakeBidResponse(LOST_LEADERSHIP, specs.auctName, bidValue)
      seller ! AuctionUpdateStatus(specs.auctName, bidValue, sender())
      buyer = sender()
      setCancelBidTimeout()
    } else {
      log.info("Auction {} got invalid bid from {}. Current leading value: {}", specs.auctName, _name, bidValue)
      sender() !  MakeBidResponse(FAILED, specs.auctName, bidValue)
    }
    allBuyersThatMadeBid += sender()
    log.info("Auction {} become ACTIVATED", specs.auctName)
    context become activated
  }

  def sold(): Receive = {
    case DeleteTimerExpired =>
      notify_parties()
      log.info("Item {} is deleting", specs.auctName)
      allBuyersThatMadeBid.foreach(buyer => buyer ! AuctionClosed(specs.auctName))
      context.stop(self)
  }

  def notify_parties(): Unit ={
    import SystemUser._
    val status = AuctionFinalStatus(specs.auctName, bidValue, seller, buyer)
    seller ! status
    buyer ! status
  }

  private def setCancelBidTimeout() : Unit = {
    cancelBidTimeout = context.system.scheduler.scheduleOnce(
      specs.bidTimeout,
      self,
      BidTimerExpired
    )
  }

  private def setDeleteBidTimeout(): Unit = {
    cancelDeleteTimeout = context.system.scheduler.scheduleOnce(
      specs.deleteTimeout,
      self,
      DeleteTimerExpired
    )
  }
}
