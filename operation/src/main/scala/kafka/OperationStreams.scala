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


    kafkaSource[AccountUpdated]
      .mapAsync(1){ e =>
        val destinationId: Int = e.destinationId.getOrElse(0)
        if (destinationId != 0) {
          val command = AccountUpdate(e.operationId, destinationId, None, -e.value, e.category)
          produceCommand(command)
          Future(println(s"При переводе №${e.operationId} на счет ${destinationId} была начислена сумма ${-e.value}."))
        } else{Future(println(" "))}
      }
      .to(Sink.ignore)
      .run()
}
