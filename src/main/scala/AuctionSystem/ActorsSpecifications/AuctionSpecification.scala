package AuctionSystem.ActorsSpecifications

import scala.concurrent.duration.FiniteDuration

object AuctionSpecification{
  def apply(auctName: String, bidTimeout : FiniteDuration, deleteTimeout : FiniteDuration,
            bidInitValue: Double = 0) = new AuctionSpecification(auctName, bidTimeout,
                                                                 deleteTimeout, bidInitValue)
}

case class AuctionSpecification(auctName: String,bidTimeout : FiniteDuration,
                            deleteTimeout : FiniteDuration, bidInitValue: Double = 0)
