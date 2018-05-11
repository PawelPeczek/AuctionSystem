package AuctionSystem.Messages
import scala.concurrent.duration._

final case class StartAuction(bidTimeout: FiniteDuration, deleteTimeout: FiniteDuration, name: String)
