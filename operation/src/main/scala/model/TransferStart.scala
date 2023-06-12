package model

case class TransferStart (sourceId: Int, destinationId: Int, value: Int, category: Option[String])
