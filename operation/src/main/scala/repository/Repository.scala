package repository

import commonkafka.TopicName
import kafka.OperationStreams
import model.{Account, AccountUpdate, TransferStart}
import io.circe.generic.auto._
import scala.concurrent.Future

class Repository(streams: OperationStreams){
    //implicit val commandTopicName: TopicName[AccountUpdate] = streams.simpleTopicName[AccountUpdate]

    def transfer(transfer: TransferStart) = {
        if (transfer.value > 0) {
            //streams.checkAccountUpdateEvent(transfer.transferId, transfer.destinationId)
            implicit val commandTopicName: TopicName[AccountUpdate] = streams.simpleTopicName[AccountUpdate]
            streams.produceCommand(AccountUpdate(transfer.transferId, transfer.sourceId, Some(transfer.destinationId), -transfer.value, transfer.category))
            println(s"При переводе №${transfer.transferId} со счета ${transfer.sourceId} была снята сумма ${-transfer.value}.")
        }
    }

}
