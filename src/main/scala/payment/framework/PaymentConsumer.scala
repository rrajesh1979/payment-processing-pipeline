package payment.framework

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import java.util.Properties

object PaymentConsumer extends App {

  val bootstrapServers = "localhost:9092"
  val topicName = "payment-ingestion"

  //Create Producer Properties
  val groupId = "payment-app"
  val consumerProperties = new Properties

  consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
  consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId)
  consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

}
