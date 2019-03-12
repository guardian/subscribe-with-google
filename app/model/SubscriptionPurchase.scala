package model

import cats.kernel.Semigroup
import cats.implicits._
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
                                profileName: Option[String],
                                emailAddress: Option[String],
                                givenName: Option[String],
                                familyName: Option[String],
                                profileId: Option[String]) {


  val customerNameOpt: Option[String] = Semigroup[Option[String]].combine(givenName.map(str => str + " "), familyName)
}

object CancelSurveyResult {
  implicit val format = Json.format[CancelSurveyResult]
}

object SubscriptionPurchase {
  implicit val format = Json.format[SubscriptionPurchase]
}
