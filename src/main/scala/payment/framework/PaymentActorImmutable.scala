package payment.framework

import akka.actor.{ActorLogging, Props}
import akka.persistence._
import com.google.protobuf.Timestamp
import payment.framework.serializers.Command.CommandProto
import payment.framework.serializers.Payment.PaymentProto

object PaymentActorImmutable {
  def props(paymentID: String,  tenantID: String,  txnDate: Timestamp,
            currentStage: String): Props =
    Props(new PaymentActor(paymentID, tenantID, txnDate, currentStage))
}
class PaymentActorImmutable(paymentID: String, tenantID: String, txnDate: Timestamp,
                            currentStage: String)
  extends PersistentActor with ActorLogging {

  import PaymentDomainModel._

  override def persistenceId: String = s"PAY-$tenantID-$paymentID"

  override def receiveCommand: Receive = receiveHandler(Payment(paymentID, tenantID, txnDate, currentStage))

  def receiveHandler(payment: Payment): Receive = {
    case Command(stage) =>
      log.info("Received command :: {}", Command(stage))

      val pipelineStages = CommandProto.PipelineStages.newBuilder()
      payment.processingPipeline.foreach(
        elem =>
          pipelineStages.addPipelineStage(
            CommandProto.Stage.newBuilder()
              .setStage(elem._1)
              .setStatus(elem._2)
          )
      )
      pipelineStages.addPipelineStage(
        CommandProto.Stage.newBuilder()
          .setStage(stage)
          .setStatus(true)
      )
      pipelineStages.build()

      val commandProto = CommandProto.newBuilder()
        .setCurrentStage(stage)
        .setProcessingPipeline(pipelineStages)

      persist(commandProto) {
        _ => log.info("Persisting command {}", stage)
          val newPayment = Payment(paymentID, tenantID, txnDate, currentStage, payment.processingPipeline + (stage -> true))
          context.become(receiveHandler(newPayment))
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
    case commandProto: CommandProto =>
      log.info("Recovered event :: {} :: {}", commandProto.getCurrentStage, commandProto.getProcessingPipeline)
      var recoveredProcessingPipeline: Map[String, Boolean] = Map()
      commandProto.getProcessingPipeline.getPipelineStageList.forEach(
        elem =>
          recoveredProcessingPipeline = recoveredProcessingPipeline + (elem.getStage -> elem.getStatus)
      )
      val recoveredPayment = Payment(paymentID, tenantID, txnDate, commandProto.getCurrentStage, recoveredProcessingPipeline)
      context.become(receiveHandler(recoveredPayment))

    case RecoveryCompleted =>
      log.info("Recovery completed")

    case SnapshotOffer(metadata, contents) =>
      log.info("Recovered Payment snapshot:: {} :: {}", metadata, contents)
      val recoveredPayment = getPayment(contents.asInstanceOf[PaymentProto])
      context.become(receiveHandler(recoveredPayment))
      log.info("Current state of payment after recovery :: {}", recoveredPayment)
  }

}