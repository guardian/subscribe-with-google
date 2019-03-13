package model

import play.api.libs.json.Json

case class CancelSurveyResult(cancelSurveyReason: Double, userInputCancelReason: String)

case class SubscriptionPurchase(kind: String,
                                startTimeMillis: Long,
                                expiryTimeMillis: Double,
                                autoRenewing: Boolean,
                                priceCurrencyCode: String,
                                priceAmountMicros: Double,
                                countryCode: String,
                                developerPayload: String,
                                paymentState: Double,
                                cancelReason: Double,
                                userCancellationTimeMillis: Double,
                                cancelSurveyResult: CancelSurveyResult,
                                orderId: String,
                                linkedPurchaseToken: String,
                                purchaseType: Double,
                                profileName: String,
                                emailAddress: Option[String],
                                givenName: String,
                                familyName: String,
                                profileId: String) {

  /*
   * Price is expressed in micro-units, where 1,000,000 micro-units represents one unit of the currency.
   * For example, if the subscription price is €1.99, priceAmountMicros is 1990000.
   */
  val priceAmount: Double = priceAmountMicros / 1000000
}

object CancelSurveyResult {
  implicit val format = Json.format[CancelSurveyResult]
}

object SubscriptionPurchase {
  implicit val format = Json.format[SubscriptionPurchase]
}
