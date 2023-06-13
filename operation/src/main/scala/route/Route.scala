package route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import commonkafka.TopicName
import kafka.OperationStreams
import model.{AccountUpdate, TransferStart}
import repository.Repository

import scala.concurrent.ExecutionContext


class Route(streams: OperationStreams, repository: Repository)(implicit ec: ExecutionContext) extends FailFastCirceSupport {

    implicit val commandTopicName: TopicName[AccountUpdate] = streams.simpleTopicName[AccountUpdate]

    def routes =
        (path("hello") & get) {
            complete("ok")
        } ~
            (path("update" / IntNumber / IntNumber / Segment / Segment) { (operationId, accountId, valueStr, category) =>
                val value = valueStr.toInt
                val command = AccountUpdate(operationId, accountId, value, Some(category))
                streams.produceCommand(command)
                complete(command)
            }) ~
            (path("transfer") & post & entity(as[TransferStart])) { transfer =>
                repository.startTransfer(transfer)
                val transferIdFound = streams.isTransferIdFound
                //Thread.sleep(5000)
                if (transferIdFound) {
                  streams.produceCommand(AccountUpdate(transfer.transferId+1, transfer.destinationId, transfer.value, transfer.category))
                } else {
                  println(
                          s"Перевод №${transfer.transferId} не выполнен! На аккаунте ${transfer.sourceId} не достаточно средств для перевода.")
                }
                complete(transfer)
            }
}


