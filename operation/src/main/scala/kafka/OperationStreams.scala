package kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import io.circe.generic.auto._
import commonkafka.WithKafka
import model.{AccountUpdate, AccountUpdated}
import repository.Repository

import scala.concurrent.{ExecutionContext, Future}

class OperationStreams()(implicit val system: ActorSystem, executionContext: ExecutionContext)
    extends WithKafka {
    override def group: String = "operation"

    /*var transferId = 0
    var destinationId = 0
    def checkAccountUpdateEvent(transferId: Int, destinationId: Int): Unit = {
      println(s"была вызвана функция checkAccountUpdateEvent ${transferId}")
      this.transferId = transferId
      this.destinationId = destinationId
    }*/

    kafkaSource[AccountUpdated]
      //.filter(event => event.operationId == transferId)
      .mapAsync(1){ e =>
        //println(s"При переводе №${e.operationId} со счета ${e.accountId} была снята сумма ${-e.value}.")
          val destinationId: Int = e.destinationId.getOrElse(0)
        val command = AccountUpdate(e.operationId, destinationId, None, -e.value, e.category)
        produceCommand(command)
        Future(println(s"При переводе №${e.operationId} на счет ${destinationId} была начислена сумма ${-e.value}."))
      }
      .to(Sink.ignore)
      .run()
}
