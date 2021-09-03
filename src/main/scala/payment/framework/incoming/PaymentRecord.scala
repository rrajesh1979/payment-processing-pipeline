package payment.framework.incoming

case class PaymentRecord(var paymentId: String, var tenantId: String, var txnDate: Long)

//import java.util.Date
//
//class PaymentRecord() {
//  private var paymentId = ""
//  private var tenantId = ""
//  private var txnDate = new Date()
//
//  def this(paymentId: String, tenantId: String, txnDate: Date) {
//    this()
//    this.paymentId = paymentId
//    this.tenantId = tenantId
//    this.txnDate = txnDate
//  }
//
//  def getPaymentId: String = paymentId
//  def getTenantId: String = tenantId
//  def getTxnDate: Date = txnDate
//}