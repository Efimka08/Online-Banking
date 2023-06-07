package kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import repository.Repository
import io.circe.generic.auto._
import model.{AccountUpdate, AccountUpdated}

import scala.concurrent.ExecutionContext

class Streams(repository: Repository)(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {

  kafkaSource[AccountUpdate]
    .mapAsync(1)(command => repository.update(command.value))
    .map(account => AccountUpdated(account.id, account.amount))
    .to(kafkaSink)
    .run()

  kafkaSource[AccountUpdated]
    .map { event =>
      println(s"Получено событие: $event")
      event
    }
    .to(Sink.ignore)
    .run()

}
