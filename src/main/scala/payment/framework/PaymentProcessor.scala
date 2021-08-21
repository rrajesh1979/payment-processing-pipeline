package payment.framework

import akka.actor.ActorSystem
import com.google.protobuf.Timestamp
import com.typesafe.config.ConfigFactory

object PaymentProcessor extends App {
  val paymentActorSystem = ActorSystem("PaymentActorSystem", ConfigFactory.load().getConfig("paymentPersistence"))
  val payment = paymentActorSystem.actorOf(
    PaymentActor.props(
      "001",
      "DEMO-TENANT",
      Timestamp.newBuilder().setSeconds(System.nanoTime()).build(),
      PaymentDomainModel.STAGE_NEW)
  )
//  payment ! PaymentDomainModel.CmdSanctionCheck
//  payment ! PaymentDomainModel.CmdAmlCheck
//  payment ! PaymentDomainModel.CmdFraudCheck

  payment ! "print"

//  payment ! "snapshot"
}
