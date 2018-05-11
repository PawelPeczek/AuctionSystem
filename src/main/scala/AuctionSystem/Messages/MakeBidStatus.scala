package AuctionSystem.Messages

case class MakeBidStatus()

case object OK extends MakeBidStatus
case object FAILED extends MakeBidStatus
case object LOST_LEADERSHIP extends MakeBidStatus

case class MakeBidResponse(status: MakeBidStatus, name: String)