package model

import cats.kernel.Semigroup
import cats.implicits._
import play.api.libs.json.Json

case class CancelSurveyResult(cancelSurveyReason: Double, userInputCancelReason: String)

case class SubscriptionPurchase(kind: String,
                                startTimeMillis: String,
                                expiryTimeMillis: String,
                                autoRenewing: Option[Boolean],
                                priceCurrencyCode: String,
                                priceAmountMicros: String,
                                countryCode: String,
                                developerPayload: String,
                                paymentState: Double,
                                cancelReason: Double,
                                userCancellationTimeMillis: Option[String],
                                cancelSurveyResult: Option[CancelSurveyResult],
                                orderId: String,
                                linkedPurchaseToken: Option[String],
                                purchaseType: Option[Double],
                                profileName: Option[String],
                                emailAddress: Option[String],
                                givenName: Option[String],
                                familyName: Option[String],
                                profileId: Option[String]){

  /*
   * Price is expressed in micro-units, where 1,000,000 micro-units represents one unit of the currency.
   * For example, if the subscription price is €1.99, priceAmountMicros is 1990000.
   */
  val priceAmount: BigDecimal = BigDecimal(priceAmountMicros) / 1000000


  val customerNameOpt: Option[String] = Semigroup[Option[String]].combine(givenName.map(str => str + " "), familyName)
}

object CancelSurveyResult {
  implicit val format = Json.format[CancelSurveyResult]
}

object SubscriptionPurchase {
  implicit val format = Json.format[SubscriptionPurchase]
}
