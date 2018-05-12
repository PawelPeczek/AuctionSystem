package AuctionSystem.ActorsSpecifications

import scala.concurrent.duration.FiniteDuration

object AuctionSpecification{
  def apply(auctName: String, bidTimeout : FiniteDuration, deleteTimeout : FiniteDuration,
            bidInitValue: Double = 0) = new AuctionSpecification(auctName, bidTimeout,
                                                                 deleteTimeout, bidInitValue)
}

class AuctionSpecification(val auctName: String, val bidTimeout : FiniteDuration,
                           val deleteTimeout : FiniteDuration, val bidInitValue: Double = 0)
