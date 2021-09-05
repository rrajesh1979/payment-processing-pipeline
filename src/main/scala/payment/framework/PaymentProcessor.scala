package payment.framework

import akka.actor.{ActorSystem, Props}
import com.google.protobuf.Timestamp
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, KafkaConsumer}
import spray.json._

import java.io.FileReader
import java.time.Duration
import java.util.{Collections, Properties}
import scala.collection.mutable.Queue
import io.circe.{Decoder, parser}
import io.circe.generic.semiauto.deriveDecoder
import payment.framework.PaymentDomainModel._
import payment.framework.incoming.PaymentRecord

import scala.collection.mutable

object PaymentProcessor extends App {
  val PARALLELISM = 100

  val paymentActorSystem = ActorSystem("PaymentActorSystem", ConfigFactory.load().getConfig("paymentPersistenceMongoDB"))

  val payment = paymentActorSystem.actorOf(
    PaymentActorImmutable.props(
      "0855",
      "DEMO-TENANT",
      Timestamp.newBuilder().setSeconds(System.nanoTime()).build(),
      PaymentDomainModel.STAGE_NEW)
  )

//  payment ! Command(STAGE_SANCTION_CHK)
//  payment ! Command(STAGE_AML_CHK)
//  payment ! Command(STAGE_FRAUD_CHK)
//  payment ! Command(STAGE_FUNDS_CONTROL_CHK)

//  payment ! "print"

//  payment ! "snapshot"

//  var paymentActorProps: mutable.Queue[Props] = getPaymentToProcess
//
//  paymentActorProps.foreach(
//    elem => {
//      val paymentActor = paymentActorSystem.actorOf(elem)
//      paymentActor ! Command(STAGE_SANCTION_CHK)
//    }
//  )

  getPaymentToProcess


  def getPaymentToProcess: mutable.Queue[Props] = {
    var paymentActorProps = mutable.Queue[Props]()
    val configFileName = "src/main/resources/kafka.config"
    val topicName = "payment-ingestion"
    val props = buildProperties(configFileName)
    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(Collections.singletonList(topicName))

    while(true) {
      println("Polling")
      val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofSeconds(1))

      records.forEach(
        record => {
          println(record)
          val key = record.key()
          val value = record.value()
          println(s"Consumed record with key $key and value $value")

          implicit val paymentDecoder: Decoder[PaymentRecord] = deriveDecoder[PaymentRecord]
          val paymentRecord = parser.decode[PaymentRecord](value)

          println(value.parseJson)
          println(paymentRecord)

          paymentRecord match {
            case Right(paymentRecord) =>
              println(paymentRecord.paymentId)
              val paymentProps = PaymentActorImmutable.props(
                paymentRecord.paymentId,
                paymentRecord.tenantId,
                Timestamp.newBuilder().setSeconds(paymentRecord.txnDate).build(),
                PaymentDomainModel.STAGE_NEW)

              val paymentActor = paymentActorSystem.actorOf(paymentProps)

              paymentActor ! Command(STAGE_SANCTION_CHK)
              paymentActor ! Command(STAGE_AML_CHK)
              paymentActor ! Command(STAGE_FRAUD_CHK)
              paymentActor ! Command(STAGE_FUNDS_CONTROL_CHK)
              paymentActor ! Command(STAGE_LIQUIDITY_CONTROL_CHK)
              paymentActor ! Command(STAGE_ACCOUNT_POSTINGS_CHK)

              paymentActorProps.enqueue(paymentProps)
            case Left(error) => println(error.getMessage)
          }
        }
      )
    }
    paymentActorProps
  }

  def buildProperties(configFileName: String): Properties = {
    val properties = new Properties()
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "payment_group")
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    properties.load(new FileReader(configFileName))
    properties
  }

}
