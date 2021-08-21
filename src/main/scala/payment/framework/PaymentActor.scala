package payment.framework

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.google.protobuf.Timestamp
import payment.framework.serializers.Event.EventProto

object PaymentActor {
  def props(paymentID: String,  tenantID: String,  txnDate: Timestamp,
            currentStage: String): Props =
    Props(new PaymentActor(paymentID, tenantID, txnDate, currentStage))
}
class PaymentActor(paymentID: String, tenantID: String, txnDate: Timestamp,
                   currentStage: String)
  extends PersistentActor with ActorLogging {

  import PaymentDomainModel._

  var payment: Payment = Payment(paymentID, tenantID, txnDate, currentStage)

  override def persistenceId: String = s"PAY-$tenantID-$paymentID"

  override def receiveCommand: Receive = {
    case Command(STAGE_SANCTION_CHK) =>
      log.info("Received command :: {}", Command(STAGE_SANCTION_CHK))
      val eventProto = EventProto.newBuilder().setStage(STAGE_SANCTION_CHK).build()
      persist(eventProto) {
          _ => log.info("Persisting command {}", STAGE_SANCTION_CHK)
          payment.currentStage = STAGE_SANCTION_CHK
          payment.processingPipeline = payment.processingPipeline + ("STAGE_SANCTION_CHK" -> true)
      }
    case Command(STAGE_AML_CHK) =>
      log.info("Received command :: {}", Command(STAGE_AML_CHK))
      val eventProto = EventProto.newBuilder().setStage(STAGE_AML_CHK).build()
      persist(eventProto) {
          _ => log.info("Persisting command {}", STAGE_AML_CHK)
          payment.currentStage = STAGE_AML_CHK
          payment.processingPipeline = payment.processingPipeline + ("STAGE_AML_CHK" -> true)
      }
    case Command(STAGE_FRAUD_CHK) =>
      log.info("Received command :: {}", Command(STAGE_FRAUD_CHK))
      val eventProto = EventProto.newBuilder().setStage(STAGE_FRAUD_CHK).build()
      persist(eventProto) {
          _ => log.info("Persisting command {}", STAGE_FRAUD_CHK)
          payment.currentStage = STAGE_FRAUD_CHK
          payment.processingPipeline = payment.processingPipeline + ("STAGE_FRAUD_CHK" -> true)
      }

    case "snapshot" =>
      log.info("Saving snapshot of payment")
      saveSnapshot(payment)

    case "print" =>
      log.info("Current state of payment :: {}", payment)

    case SaveSnapshotSuccess(metadata) =>
      log.info("Snapshot save was successful :: {}", metadata)
    case SaveSnapshotFailure(_, cause) =>
      log.info("Save snapshot failed :: {}", cause)
  }

  override def receiveRecover: Receive = {
    case eventProto: EventProto =>
      log.info("Recovered event :: {}", eventProto.getStage)
      eventProto.getStage match {
        case STAGE_SANCTION_CHK =>
          payment.currentStage = STAGE_SANCTION_CHK
          payment.processingPipeline = payment.processingPipeline + ("STAGE_SANCTION_CHK" -> true)
        case STAGE_AML_CHK =>
          payment.currentStage = STAGE_AML_CHK
          payment.processingPipeline = payment.processingPipeline + ("STAGE_AML_CHK" -> true)
        case STAGE_FRAUD_CHK =>
          payment.currentStage = STAGE_FRAUD_CHK
          payment.processingPipeline = payment.processingPipeline + ("STAGE_FRAUD_CHK" -> true)
      }

    case RecoveryCompleted =>
      log.info("Recovery completed")

    case SnapshotOffer(metadata, contents) =>
      log.info("Recovered Payment snapshot:: {} :: {}", metadata, contents)
      payment = contents.asInstanceOf[Payment]
      log.info("Current state of payment after recovery :: {}", payment)
  }

}

object PaymentDomainModel {
  case class Payment(var paymentID: String, var tenantID: String, var txnDate: Timestamp,
                     var currentStage: String, var processingPipeline: Map[String, Boolean] = starterPipeline)

  //commands
  case class Command(stage: String)

  //events
  case class Event(stage: String)

  //Payment stages
  val STAGE_NEW = "STAGE_NEW"
  val STAGE_SANCTION_CHK = "STAGE_SANCTION_CHK"
  val STAGE_AML_CHK = "STAGE_AML_CHK"
  val STAGE_FRAUD_CHK = "STAGE_FRAUD_CHK"
  val STAGE_FUNDS_CONTROL_CHK = "STAGE_FUNDS_CONTROL_CHK"
  val STAGE_LIQUIDITY_CONTROL_CHK = "STAGE_LIQUIDITY_CONTROL_CHK"
  val STAGE_ACCOUNT_POSTINGS_CHK = "STAGE_ACCOUNT_POSTINGS_CHK"
  val STAGE_PROCESSING_COMPLETE = "STAGE_PROCESSING_COMPLETE"

  val starterPipeline = Map(
    STAGE_NEW -> true,
//    STAGE_SANCTION_CHK -> false,
//    STAGE_AML_CHK -> false,
//    STAGE_FRAUD_CHK -> false,
//    STAGE_FUNDS_CONTROL_CHK -> false,
//    STAGE_LIQUIDITY_CONTROL_CHK -> false,
//    STAGE_ACCOUNT_POSTINGS_CHK -> false,
//    STAGE_PROCESSING_COMPLETE -> false
  )
}

