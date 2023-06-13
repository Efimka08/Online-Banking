package kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import io.circe.generic.auto._
import commonkafka.WithKafka
import model.{AccountUpdate, AccountUpdated}
import repository.Repository

import scala.concurrent.ExecutionContext

class OperationStreams()(implicit val system: ActorSystem, executionContext: ExecutionContext)
    extends WithKafka {
    override def group: String = "operation"

    var transferIdFound = false
    def isTransferIdFound: Boolean = transferIdFound

    /*kafkaSource[AccountUpdate]
      //.filter(command => repository.getAccount(command.accountId).exists(_.amount + command.value >= 0))
      .mapAsync(1) { command =>
        repository
          .update(command.accountId, command.value)
          .map(_ => AccountUpdated(
            accountId = command.accountId,
            value = command.value,
            category = command.category))
      }
      .to(kafkaSink)
      .run()*/

    def checkAccountUpdateEvent(transferId: Int): Unit = {
      println(s"была вызвана функция checkAccountUpdateEvent ${transferId}")
      kafkaSource[AccountUpdated]
        .filter(event => event.operationId == transferId)
        .map { e =>
          transferIdFound = true
          println(s"При переводе №${e.operationId} с аккаунта ${e.accountId} была снята сумма ${e.value}.")
          e
        }
        .to(Sink.ignore)
        .run()
    }
}
