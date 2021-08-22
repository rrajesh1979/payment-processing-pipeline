package payment.framework

import akka.actor.ActorSystem
import com.google.protobuf.Timestamp
import com.typesafe.config.ConfigFactory
import payment.framework.PaymentDomainModel._

object PaymentProcessor extends App {
  val paymentActorSystem = ActorSystem("PaymentActorSystem", ConfigFactory.load().getConfig("paymentPersistence"))
  val payment = paymentActorSystem.actorOf(
    PaymentActor.props(
      "005",
      "DEMO-TENANT",
      Timestamp.newBuilder().setSeconds(System.nanoTime()).build(),
      PaymentDomainModel.STAGE_NEW)
  )
//  payment ! Command(STAGE_SANCTION_CHK)
//  payment ! Command(STAGE_AML_CHK)
//  payment ! Command(STAGE_FRAUD_CHK)
//  payment ! Command(STAGE_FUNDS_CONTROL_CHK)

  payment ! "print"

//  payment ! "snapshot"
}
