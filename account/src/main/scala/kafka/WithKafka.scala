package kafka

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Flow, Source}
import io.circe.{Decoder, Encoder}
import io.circe._
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.parser._
import io.circe.syntax._
import model.AccountUpdate
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.reflect.ClassTag

trait WithKafka {
  implicit def system: ActorSystem

  val consumerConfig = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

  val producerConfig = system.settings.config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def defineTopic[T](implicit tag: ClassTag[T]) = tag.runtimeClass.getSimpleName

  def kafkaSource[T](implicit decoder: Decoder[T], tag: ClassTag[T]) = Consumer.committableSource(consumerSettings, Subscriptions.topics(defineTopic[T]))
    .map(message => message.record.value())
    .map(body => decode[T](body))
    .collect {
      case Right(command) => command
      case Left(error) => throw new RuntimeException(s"Ошибка при разборе сообщения $error")
    }

  def kafkaSink[T](implicit encoder: Encoder[T], tag: ClassTag[T]) = Flow[T]
    .map(event => event.asJson.noSpaces)
    .map(value => new ProducerRecord[String, String](defineTopic[T], value))
    .log(s"Случилась ошибка при чтении топика ${defineTopic[T]}")
    .to(Producer.plainSink(producerSettings))

  def produceCommand(command: AccountUpdate) = {
    Source.single(command)
      .map(command => command.asJson.noSpaces)
      .map(value => new ProducerRecord[String, String](defineTopic[AccountUpdate], value))
      .to(Producer.plainSink(producerSettings))
      .run()
  }

}
