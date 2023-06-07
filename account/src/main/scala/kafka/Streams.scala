package kafka

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, StringSerializer}
import repository.Repository
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import model.{AccountUpdate, AccountUpdated}

import scala.concurrent.ExecutionContext

class Streams(repository: Repository)(implicit val system: ActorSystem, executionContext: ExecutionContext) {

  val consumerConfig = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

  val producerConfig = system.settings.config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private val topicUpdate = "account-update"
  private val topicUpdated = "account-updated"
  Consumer.committableSource(consumerSettings, Subscriptions.topics(topicUpdate))
    .map(message => message.record.value())
    .map(body => decode[AccountUpdate](body))
    .collect {
      case Right(command) => command
      case Left(error) => throw new RuntimeException(s"Ошибка при разборе сообщения $error")
    }
    .mapAsync(1)(command => repository.update(command.value))
    .map(account => AccountUpdated(account.id, account.amount))
    .map(event => event.asJson.noSpaces)
    .map(value => new ProducerRecord[String, String](topicUpdated, value))
    .log(s"Случилась ошибка при чтении топика $topicUpdate")
    .to(Producer.plainSink(producerSettings))
    .run()

  Consumer.committableSource(consumerSettings, Subscriptions.topics(topicUpdated))
    .map(message => message.record.value())
    .map(body => decode[AccountUpdated](body))
    .map { event =>
      println(s"Получено событие: $event")
      event
    }
    .log(s"Случилась ошибка при чтении топика $topicUpdated")
    .to(Sink.ignore)
    .run()

  def produceCommand(command: AccountUpdate) = {
    Source.single(command)
      .map(command => command.asJson.noSpaces)
      .map(value => new ProducerRecord[String, String](topicUpdate, value))
      .to(Producer.plainSink(producerSettings))
      .run()
  }

}
