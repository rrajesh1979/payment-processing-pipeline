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
  )

  def getPayment(paymentProto: PaymentProto): Payment = {
    var processingPipeline: Map[String, Boolean] = Map()
    paymentProto.getProcessingPipeline.getPipelineStageList.forEach(
      elem =>
        processingPipeline = processingPipeline + (elem.getStage -> elem.getStatus)
    )

    val payment: Payment = Payment(
      paymentProto.getPaymentID,
      paymentProto.getTenantID,
      paymentProto.getTxnDate,
      paymentProto.getCurrentStage,
      processingPipeline
    )
    payment
  }

  def getPaymentProto(payment: Payment): PaymentProto = {

    val pipelineStages = PaymentProto.PipelineStages.newBuilder()
    payment.processingPipeline.foreach(
      elem =>
        pipelineStages.addPipelineStage(
          PaymentProto.Stage.newBuilder()
            .setStage(elem._1)
            .setStatus(elem._2)
        ).build()
    )

    pipelineStages.build()

    val paymentProto = PaymentProto.newBuilder()
      .setPaymentID(payment.paymentID)
      .setTenantID(payment.tenantID)
      .setTxnDate(payment.txnDate)
      .setCurrentStage(payment.currentStage)
      .setProcessingPipeline(pipelineStages)
      .build()

    paymentProto
  }
}

