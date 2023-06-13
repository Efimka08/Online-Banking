package repository

import commonkafka.TopicName
import kafka.OperationStreams
import model.{Account, AccountUpdate, TransferStart}
import io.circe.generic.auto._
import scala.concurrent.Future

class Repository(streams: OperationStreams){
    //implicit val commandTopicName: TopicName[AccountUpdate] = streams.simpleTopicName[AccountUpdate]

    def startTransfer(transfer: TransferStart) = {
        if (transfer.value > 0) {
            implicit val commandTopicName: TopicName[AccountUpdate] = streams.simpleTopicName[AccountUpdate]
            streams.produceCommand(AccountUpdate(transfer.transferId, transfer.sourceId, -transfer.value, transfer.category))

            streams.checkAccountUpdateEvent(transfer.transferId)
          /*
          streams.produceCommand(AccountUpdate(transfer.transferId, transfer.destinationId, transfer.value, transfer.category))*/
        }
    }

    /*def continueTransfer(transfer: TransferStart) = {
        if (transfer.value > 0) {
            implicit val commandTopicName: TopicName[AccountUpdate] = streams.simpleTopicName[AccountUpdate]
            //streams.produceCommand(AccountUpdate(transfer.transferId, transfer.sourceId, -transfer.value, transfer.category))
            streams.produceCommand(AccountUpdate(transfer.transferId, transfer.destinationId, transfer.value, transfer.category))
        }
    }*/
}
