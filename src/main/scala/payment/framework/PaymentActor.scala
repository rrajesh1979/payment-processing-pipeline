package payment.framework

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.google.protobuf.Timestamp

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
    case CmdSanctionCheck =>
      log.info("Received command :: CmdSanctionCheck")
      persist(STAGE_SANCTION_CHK) {
        _ => log.info("Persisting command {}", STAGE_SANCTION_CHK)
        payment.currentStage = STAGE_SANCTION_CHK
      }
    case CmdAmlCheck =>
      log.info("Received command :: CmdAmlCheck")
      payment.currentStage = STAGE_AML_CHK
    case CmdFraudCheck =>
      payment.currentStage = STAGE_FRAUD_CHK
      log.info("Received command :: CmdFraudCheck")

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
    case EventSanctionCheck =>
      payment.currentStage = STAGE_SANCTION_CHK
      log.info("Recovered event :: EventSanctionCheck")
    case EventAmlCheck =>
      payment.currentStage = STAGE_AML_CHK
      log.info("Recovered event :: EventAmlCheck")
    case EventFraudCheck =>
      payment.currentStage = STAGE_FRAUD_CHK
      log.info("Recovered event :: EventFraudCheck")

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
                     var currentStage: String)

  //commands
  case class CmdSanctionCheck()
  case class CmdAmlCheck()
  case class CmdFraudCheck()

  //events
  case class EventSanctionCheck()
  case class EventAmlCheck()
  case class EventFraudCheck()

  //Payment stages
  val STAGE_NEW = "STAGE_NEW"
  val STAGE_SANCTION_CHK = "STAGE_SANCTION_CHK"
  val STAGE_AML_CHK = "STAGE_AML_CHK"
  val STAGE_FRAUD_CHK = "STAGE_FRAUD_CHK"
  val STAGE_FUNDS_CONTROL_CHK = "STAGE_FUNDS_CONTROL_CHK"
  val STAGE_LIQUIDITY_CONTROL_CHK = "STAGE_LIQUIDITY_CONTROL_CHK"
  val STAGE_ACCOUNT_POSTINGS_CHK = "STAGE_ACCOUNT_POSTINGS_CHK"
  val STAGE_PROCESSING_COMPLETE = "STAGE_PROCESSING_COMPLETE"

}