package model
import play.api.libs.json.Json

case class PaymentRecord(id: String)

object PaymentRecord {
  implicit val format = Json.format[PaymentRecord]
}
