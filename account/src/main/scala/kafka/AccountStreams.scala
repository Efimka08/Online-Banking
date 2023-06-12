package kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import io.circe.generic.auto._
import commonkafka.WithKafka
import model.{AccountUpdate, AccountUpdated}
import repository.Repository

import scala.concurrent.ExecutionContext

class AccountStreams(repository: Repository)(implicit val system: ActorSystem, executionContext: ExecutionContext)
    extends WithKafka {

    def group = s"account-${repository.accountId}"

    kafkaSource[AccountUpdate]
        .filter(command => repository.account.id == command.accountId && repository.account.amount + command.value >= 0)
        .mapAsync(1) { command =>
            repository.update(command.value).map(_ => AccountUpdated(command.accountId, command.value))
        }
        .to(kafkaSink)
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
