package payment.framework

import com.google.protobuf.Timestamp
import payment.framework.serializers.Payment.PaymentProto

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
