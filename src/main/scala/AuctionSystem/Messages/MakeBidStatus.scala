package AuctionSystem.Messages

class MakeBidStatus()

case object StartBid
case object OK extends MakeBidStatus
case object FAILED extends MakeBidStatus
case object LOST_LEADERSHIP extends MakeBidStatus

case class MakeBidResponse(status: MakeBidStatus, name: String)