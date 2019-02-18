package model
import play.api.libs.json.Json


case class PaymentRecord(firstName: String,
                         email: String,
                         status: PaymentStatus,
                         amount: BigDecimal,
                         currency: String,
                         countryCode: String,
                         paymentId: String,
                         receivedTimestamp: Long)

object PaymentRecord {
  implicit val format = Json.format[PaymentRecord]
}
