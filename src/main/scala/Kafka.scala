import akka.actor.ActorSystem
import akka.kafka.{ ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions }
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import cakesolutions.kafka.testkit.KafkaServer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }

import scala.concurrent.duration._
import scala.language.postfixOps

object Kafka extends App {
  // Needed for streams to leverage actors for their functionality
  implicit val system = ActorSystem("MyActorSystemForKafka")
  implicit val materializer = ActorMaterializer()

  val kafkaPort = 8989
  val kafkaLocation = s"localhost:$kafkaPort"
  val kafkaGroupId = "myGroup"
  val kafkaTopic = "myTopic"

  new KafkaServer(kafkaPort = kafkaPort).startup()

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaLocation)
    .withGroupId(kafkaGroupId)
  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(kafkaLocation)

  val kafkaSource = {
//    Consumer.plainSource(consumerSettings, Subscriptions.topics(kafkaTopic))
    Source.fromPublisher(
      Consumer
        .plainSource(consumerSettings, Subscriptions.topics(kafkaTopic))
        .runWith(Sink.asPublisher(fanout = true))
    )
  }
  val kafkaSink = Producer.plainSink(producerSettings)



  // A stream which periodically produces messages to Kafka
  val stream1 = Source
    .tick(0 seconds, 2 seconds, "Hello World!")
    .map(msg => new ProducerRecord[String, String](kafkaTopic, msg))
    .to(kafkaSink)
  stream1.run()



  // A stream which listens to messages from Kafka and sends them to the standard out
  val stream2 = kafkaSource.to(Sink.foreach(record => println(record.value())))
  stream2.run()



  // A stream which listens to messages from Kafka, modifies them then sends them back to Kafka
  val toAdd = "!!1!"
  val stream3 = kafkaSource
    .map(_.value())
    .filterNot(_.contains(toAdd))
    .map(_ + toAdd)
    .map(msg => new ProducerRecord[String, String](kafkaTopic, msg))
    .to(kafkaSink)
  stream3.run()
}
