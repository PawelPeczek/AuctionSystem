package AuctionSystem.Messages

import scala.concurrent.duration.FiniteDuration

final case class MakeAuction(auctName: String, auctPathName: String,  bidTimeout : FiniteDuration, deleteTimeout : FiniteDuration,
                             bidValue: Double = 0)
