import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import reactivemongo.api.bson.{BSONDateTime, BSONDocument, BSONString}
import java.util.Date

object PaymentProcBSONApp extends App {

  case class Payment( paymentID: String,  tenantID: String,  txnDate: Date,
                      currentStage: String,  processingPipeline: Map[String, Boolean]) {
    def getBson: BSONDocument = {
      val bsonDoc = BSONDocument(
        "paymentId" -> BSONString(paymentID),
        "tenantId" -> BSONString(tenantID),
        "txnDate" -> BSONDateTime(txnDate.getTime),
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

    override def persistenceId: String = "payment-id-bson"
  }

  val paymentSystem = ActorSystem("payment-pipeline-system")
  val paymentProcessorActor = paymentSystem.actorOf(Props[PaymentProcessor], "paymentProcessorBson")

  for(i <- 1 to 10) {
    paymentProcessorActor ! Payment("PAY-"+i, "DEMO-TENANT", new Date, "NEW", starterPipeline)
  }

}
