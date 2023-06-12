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
        .filter(command => repository.getAccount(command.accountId).exists(_.amount + command.value >= 0))
        .mapAsync(1) { command =>
            repository
              .update(command.accountId, command.value)
              .map(_ => AccountUpdated(
                  accountId = command.accountId,
                  value = command.value,
                  category = command.category))
        }
        .to(kafkaSink)
        .run()

    kafkaSource[AccountUpdated]
        .filter(event => repository.getAccount(event.accountId).nonEmpty)
        .map { e =>
            println(s"Аккаунт ${e.accountId} обновлен на сумму ${e.value}. Баланс: ${repository.getAccount(e.accountId).get.amount}")
            e
        }
        .to(Sink.ignore)
        .run()
}
