package payment.framework

import akka.actor.{ActorLogging, ActorSystem, ClassicActorSystemProvider, Props}
import akka.persistence._
import akka.stream.scaladsl.{Sink, Source}
import com.google.protobuf.Timestamp
import payment.framework.serializers.Command.CommandProto
import payment.framework.serializers.Command.CommandProto.PipelineStages
import payment.framework.serializers.Payment.PaymentProto
import spray.json.DefaultJsonProtocol

import scala.util.{Failure, Success}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, Materializer, SystemMaterializer}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import spray.json._


case class PaymentRequest(paymentId: String, tenantId: String, txnDate: Long, paymentDetails: String)

trait PaymentRequestJsonProtocol extends DefaultJsonProtocol {
  implicit val paymentJSON = jsonFormat4(PaymentRequest)
}

object PaymentActorImmutable {
  def props(paymentID: String,  tenantID: String,  txnDate: Timestamp,
            currentStage: String): Props =
    Props(new PaymentActorImmutable(paymentID, tenantID, txnDate, currentStage))
}
class PaymentActorImmutable(paymentID: String, tenantID: String, txnDate: Timestamp,
                            currentStage: String)
  extends PersistentActor with ActorLogging with PaymentRequestJsonProtocol {

  import PaymentDomainModel._

  implicit val mockServiceSystem = ActorSystem("MockServiceActorSystem", ConfigFactory.load().getConfig("paymentPersistenceMongoDB"))
  implicit def matFromSystem(implicit provider: ClassicActorSystemProvider): Materializer =
    SystemMaterializer(provider.classicSystem).materializer
  import mockServiceSystem.dispatcher


  override def persistenceId: String = s"PAY-$tenantID-$paymentID"

  override def receiveCommand: Receive = receiveHandler(Payment(paymentID, tenantID, txnDate, currentStage))

  def receiveHandler(payment: Payment): Receive = {
    case Command(stage) => {
      log.info("Received command :: {}", Command(stage))

      val pipelineStages: PipelineStages.Builder = getUpdatedPipelineStages(payment, stage)

      val commandProto = CommandProto.newBuilder()
        .setCurrentStage(stage)
        .setProcessingPipeline(pipelineStages)
        .build()

      persist(commandProto) {
        _ =>
          log.info("Persisting command {}", stage)
          val newPayment = Payment(paymentID, tenantID, txnDate, currentStage, payment.processingPipeline + (stage -> true))
          context.become(receiveHandler(newPayment))
      }

      //Call Mock API

      val paymentRequest = PaymentRequest(paymentID, tenantID, txnDate.getSeconds, "Request from Actor using HTTP Client")
      var uri: String = ""
      stage match {
        case STAGE_SANCTION_CHK =>
          uri = "/api/sanctioncheck"
        case STAGE_AML_CHK =>
          uri = "/api/amlcheck"
        case STAGE_FRAUD_CHK =>
          uri = "/api/fraudcheck"
        case STAGE_LIQUIDITY_CONTROL_CHK =>
          uri = "/api/liquiditycheck"
        case STAGE_FUNDS_CONTROL_CHK =>
          uri = "/api/fundscheck"
        case STAGE_ACCOUNT_POSTINGS_CHK =>
          uri = "/api/accountpostingscheck"
      }

      val paymentHttpRequest =
        HttpRequest(
          HttpMethods.POST,
          uri = Uri(uri),
          entity = HttpEntity(
            ContentTypes.`application/json`,
            paymentRequest.toJson.prettyPrint
          )
        )

      val connectionFlowPayment = Http().outgoingConnection("localhost", 8080)

      def oneOffPaymentRequest(request: HttpRequest) =
        Source.single(request).via(connectionFlowPayment).runWith(Sink.head)

      oneOffPaymentRequest(paymentHttpRequest).onComplete {
        case Success(response) =>
          println(s"Got successful response: $response")
        case Failure(ex) =>
          println(s"Sending the request failed: $ex")
      }
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

  private def getUpdatedPipelineStages(payment: Payment, stage: String): PipelineStages.Builder = {
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
    pipelineStages
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