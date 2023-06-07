package kafka

import akka.actor.ActorSystem
import io.circe.generic.auto._

import akka.stream.scaladsl.Sink
import commonkafka.WithKafka
import repository.Repository

import model.{AccountUpdate, AccountUpdated}

import scala.concurrent.ExecutionContext

class Streams(repository: Repository)(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {

  def group = s"account-${repository.accountId}"

  kafkaSource[AccountUpdate]
    .filter(command => repository.account.id == command.accountId && repository.account.amount + command.value >= 0)
    .mapAsync(1) { command =>
      repository.update(command.value).map(_ => AccountUpdated(command.accountId, command.value))
    }
    .run()

  kafkaSource[AccountUpdated]
    .filter(event => repository.account.id == event.accountId)
    .map { e =>
      println(s"Аккаунт ${e.accountId} обновлен на сумму ${e.value}. Баланс: ${repository.account.amount}")
      e
    }
    .to(Sink.ignore)
    .run()

}
