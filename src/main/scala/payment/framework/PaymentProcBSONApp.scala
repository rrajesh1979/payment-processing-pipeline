package payment.framework

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import reactivemongo.api.bson.{BSONDateTime, BSONDocument, BSONString}

import java.util.Date

object PaymentProcBSONApp extends App {
  val STAGE_NEW = "STAGE_NEW"
  val STAGE_SANCTION_CHK = "STAGE_SANCTION_CHK"
  val STAGE_AML_CHK = "STAGE_AML_CHK"
  val STAGE_FRAUD_CHK = "STAGE_FRAUD_CHK"
  val STAGE_FUNDS_CONTROL_CHK = "STAGE_FUNDS_CONTROL_CHK"
  val STAGE_LIQUIDITY_CONTROL_CHK = "STAGE_LIQUIDITY_CONTROL_CHK"
  val STAGE_ACCOUNT_POSTINGS_CHK = "STAGE_ACCOUNT_POSTINGS_CHK"
  val STAGE_PROCESSING_COMPLETE = "STAGE_PROCESSING_COMPLETE"

  val KEY_PAYMENT_ID = "paymentId"
  val KEY_TENANT_ID = "tenantId"
  val KEY_TXN_DATE = "txnDate"
  val KEY_CURRENT_STAGE = "currentStage"
  val KEY_PROCESSING_PIPELINE = "processingPipeline"

  final val starterPipeline = Map(
    STAGE_SANCTION_CHK -> false,
    STAGE_AML_CHK -> false,
    STAGE_FRAUD_CHK -> false,
    STAGE_FUNDS_CONTROL_CHK -> false,
    STAGE_LIQUIDITY_CONTROL_CHK -> false,
    STAGE_ACCOUNT_POSTINGS_CHK -> false,
    STAGE_PROCESSING_COMPLETE -> false
  )

  case class CommandSanctionCheck(payment: Payment)
  case class CommandAmlCheck(payment: Payment)
  case class CommandFraudCheck(payment: Payment)
  case class CommandFundsCtrlCheck(payment: Payment)
  case class CommandLiquidityCtrlCheck(payment: Payment)
  case class CommandAccountPostingsCheck(payment: Payment)
  case class CommandProcessingComplete(payment: Payment)

  case class Payment(paymentID: String, tenantID: String, txnDate: Date,
                     currentStage: String, processingPipeline: Map[String, Boolean] = starterPipeline) {
    def getBson: BSONDocument = {
      val bsonDoc = BSONDocument(
        KEY_PAYMENT_ID -> BSONString(paymentID),
        KEY_TENANT_ID -> BSONString(tenantID),
        KEY_TXN_DATE -> BSONDateTime(txnDate.getTime),
        KEY_CURRENT_STAGE -> BSONString(currentStage),
        KEY_PROCESSING_PIPELINE -> processingPipeline,
      )
      bsonDoc
    }

  }

  class PaymentProcessor extends PersistentActor with ActorLogging {
    override def receiveRecover: Receive = {
      case payment: BSONDocument =>
        log.info("Recovered payment :: {}", payment.get(KEY_PROCESSING_PIPELINE))
    }

    override def receiveCommand: Receive = {
      case payment: Payment =>
        persist(payment.getBson) {
          payment => log.info("Received payment :: {}", payment.toMap)
        }
    }

    override def persistenceId: String = "payment-id"

  }

  val paymentSystem = ActorSystem("payment-pipeline-system")
  val paymentProcessorActor = paymentSystem.actorOf(Props[PaymentProcessor], "paymentProcessorBson")

//  for (i <- 1 to 10) {
//    paymentProcessorActor ! Payment("PAY-" + i, "DEMO-TENANT", new Date, STAGE_NEW)
//  }

}
