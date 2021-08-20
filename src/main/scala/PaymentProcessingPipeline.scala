import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.serialization.Serializer
import com.google.protobuf.Timestamp
import reactivemongo.api.bson.{BSONDateTime, BSONDocument, BSONString}
import payment.model.PaymentOuterClass
import payment.model.PaymentOuterClass.Payment

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.Date

object PaymentProcessingPipeline extends App {

  case class Payment( paymentID: String,  tenantID: String,  txnDate: Timestamp,
                      currentStage: String,  processingPipeline: Map[String, Boolean]) {
    def getBson: BSONDocument = {
      val bsonDoc = BSONDocument(
        "paymentId" -> BSONString(paymentID),
        "tenantId" -> BSONString(tenantID),
        "txnDate" -> BSONDateTime(txnDate.getSeconds),
        "currentStage" -> BSONString(currentStage),
        "processingPipeline" -> processingPipeline,
      )
      bsonDoc
    }
  }

  val starterPipeline = Map(
    "SANCTION_CHK" -> false,
    "AML_CHK" -> false,
    "FRAUD_CHK" -> false,
    "FUNDS_CONTROL_CHK" -> false,
    "LIQUIDITY_CONTROL_CHK" -> false,
    "ACCOUNT_POSTINGS_CHK" -> false
  )

  class PaymentProcessor extends PersistentActor with ActorLogging {
    override def receiveRecover: Receive = {
      case payment: BSONDocument =>
          log.info("Recovered payment :: {}", payment)
    }

    override def receiveCommand: Receive = {
      case payment: Payment =>
        persist( payment.getBson )
        {
          payment => log.info("Received payment :: {}", payment)
        }
    }

    override def persistenceId: String = "payment-id"
  }

  val paymentSystem = ActorSystem("payment-pipeline-system")
  val paymentProcessorActor = paymentSystem.actorOf(Props[PaymentProcessor], "paymentProcessor")

//  paymentProcessorActor ! Payment("PAY-0000000001", "DEMO-TENANT", new Date, "NEW", starterPipeline)
  //new Timestamp()

  def getPaymentProto(payment: Payment) = {
    val paymentProto = PaymentOuterClass.Payment.newBuilder()
      .setPaymentID(payment.paymentID)
      .setTenantID(payment.tenantID)
      .setTxnDate(payment.txnDate)
      .setCurrentStage(payment.currentStage)
      .setProcessingPipeline(
        1, PaymentOuterClass.Payment.PipelineStage.newBuilder().setStage("SANCTION_CHK").setStatus(false).build()
      )
      .setProcessingPipeline(
        2, PaymentOuterClass.Payment.PipelineStage.newBuilder().setStage("AML_CHK").setStatus(false).build()
      )
      .setProcessingPipeline(
        3, PaymentOuterClass.Payment.PipelineStage.newBuilder().setStage("FRAUD_CHK").setStatus(false).build()
      )
      .setProcessingPipeline(
        4, PaymentOuterClass.Payment.PipelineStage.newBuilder().setStage("FUNDS_CONTROL_CHK").setStatus(false).build()
      )
      .setProcessingPipeline(
        5, PaymentOuterClass.Payment.PipelineStage.newBuilder().setStage("LIQUIDITY_CONTROL_CHK").setStatus(false).build()
      )
      .setProcessingPipeline(
        6, PaymentOuterClass.Payment.PipelineStage.newBuilder().setStage("ACCOUNT_POSTINGS_CHK").setStatus(false).build()
      )
      .setProcessingPipeline(
        7, PaymentOuterClass.Payment.PipelineStage.newBuilder().setStage("WRITE_OUTPUT_CHK").setStatus(false).build()
      )
      .build()
    paymentProto
  }

}
