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
                                profileId: String)

object CancelSurveyResult {
  implicit val format = Json.format[CancelSurveyResult]
}

object SubscriptionPurchase {
  implicit val format = Json.format[SubscriptionPurchase]
}
