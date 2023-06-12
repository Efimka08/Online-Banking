package model

case class TransferStart (transferId: Int, sourceId: Int, destinationId: Int, value: Int, category: Option[String])
