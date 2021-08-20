import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import com.google.protobuf.Timestamp
import payment.model.PaymentOuterClass.Payment

object PaymentProcApp extends App {

  class PaymentProcessor extends PersistentActor with ActorLogging {
    override def receiveRecover: Receive = {
      case payment: Payment =>
        log.info("Recovered payment :: {}", payment.getPaymentID)
    }

    override def receiveCommand: Receive = {
      case payment: Payment =>
        persist( payment )
        {
          payment => log.info("Received payment :: {}", payment)
        }
    }

    override def persistenceId: String = "payment-id"
  }

  val paymentSystem = ActorSystem("payment-pipeline-system")
  val paymentProcessorActor = paymentSystem.actorOf(Props[PaymentProcessor], "paymentProcessor")

//  for(i <- 1 to 10) {
//    paymentProcessorActor ! getPaymentProto("PAY-"+i, "DEMO-TENANT", Timestamp.newBuilder().setSeconds(System.nanoTime()).build(), "NEW")
//  }

  def getPaymentProto(paymentID: String,  tenantID: String,  txnDate: Timestamp,
                      currentStage: String): Payment = {

    val paymentProto = Payment.newBuilder()
      .setPaymentID(paymentID)
      .setTenantID(tenantID)
      .setTxnDate(txnDate)
      .setCurrentStage(currentStage)
      .build()
    paymentProto
  }
}
