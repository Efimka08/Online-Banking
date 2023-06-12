package repository

import commonkafka.TopicName
import kafka.OperationStreams
import model.{Account, AccountUpdate, TransferStart}
import io.circe.generic.auto._
import scala.concurrent.Future

class Repository(streams: OperationStreams){
    implicit val commandTopicName: TopicName[AccountUpdate] = streams.simpleTopicName[AccountUpdate]

    def transfer(transfer: TransferStart) = {
        if (transfer.value > 0) {
            streams.produceCommand(AccountUpdate(transfer.sourceId, -transfer.value))
            streams.produceCommand(AccountUpdate(transfer.destinationId, transfer.value))
        }
    }
}
