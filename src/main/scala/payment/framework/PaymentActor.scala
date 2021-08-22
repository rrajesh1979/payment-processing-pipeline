package payment.framework

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.google.protobuf.Timestamp
import payment.framework.serializers.Event.EventProto
import payment.framework.serializers.Payment.PaymentProto

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
    case Command(stage) =>
      log.info("Received command :: {}", Command(stage))
      val eventProto = EventProto.newBuilder().setStage(stage).build()
      persist(eventProto) {
        _ => log.info("Persisting command {}", stage)
          payment.currentStage = stage
          payment.processingPipeline = payment.processingPipeline + (stage -> true)
      }

    case "snapshot" =>
      log.info("Saving snapshot of payment")
      saveSnapshot(getPaymentProto(payment))

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
      payment.currentStage = eventProto.getStage
      payment.processingPipeline = payment.processingPipeline + (eventProto.getStage -> true)

    case RecoveryCompleted =>
      log.info("Recovery completed")

    case SnapshotOffer(metadata, contents) =>
      log.info("Recovered Payment snapshot:: {} :: {}", metadata, contents)
      payment = getPayment(contents.asInstanceOf[PaymentProto])
      log.info("Current state of payment after recovery :: {}", payment)
  }

}