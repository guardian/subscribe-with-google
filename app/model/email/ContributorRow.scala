package model.email
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import play.api.libs.json.Json

object SubscriberAttributesSqsMessage {
  implicit val formats = Json.format[SubscriberAttributesSqsMessage]
}


object ContactAttributesSqsMessage {
  implicit val formats = Json.format[ContactAttributesSqsMessage]
}
object ToSqsMessage {
  implicit val formats = Json.format[ToSqsMessage]
}

object ContributorRowSqsMessage {
  implicit val formats = Json.format[ContributorRowSqsMessage]
}

object ContributorRow {
  implicit val formats = Json.format[ContributorRow]
}

//todo: Write tests to confirm serialization is correct (The weird casing means it wont be)

case class ContributorRow(
    email: String,
    currency: String,
    identityId: Long,
    firstName: Option[String],
    amount: BigDecimal
) {

  val PaymentMethod = "Subscribe with Google" //todo: Change when branding comes through

  private def edition: String = currency match {
    case "GBP" => "uk"
    case "USD" => "us"
    case "AUD" => "au"
    case _     => "international"
  }

  private val currencyGlyph: String = currency match {
    case "GBP" => "£"
    case "EUR" => "€"
    case "AUD" => "AU$"
    case "CAD" => "CA$"
    case "NZD" => "NZ$"
    case _     => "$"
  }

  private def formattedDate: String = new SimpleDateFormat("d MMMM yyyy").format(Date.from(Instant.now))

  def toJsonContributorRowSqsMessage: String = {
    Json.stringify(
      Json.toJson(ContributorRowSqsMessage(
        To = ToSqsMessage(
          Address = email,
          SubscriberKey = email,
          ContactAttributes = ContactAttributesSqsMessage(
            SubscriberAttributes = SubscriberAttributesSqsMessage(
              EmailAddress = email,
              edition = edition,
              `payment method` = PaymentMethod,
              currency = currencyGlyph,
              amount = amount.setScale(2).toString,
              first_name = firstName,
              date_of_payment = formattedDate
            )
          )
        ),
        DataExtensionName = "contribution-thank-you",
        IdentityUserId = identityId.toString
      )))
  }

}

case class ContributorRowSqsMessage(
    To: ToSqsMessage,
    DataExtensionName: String,
    IdentityUserId: String
)

case class ToSqsMessage(
    Address: String,
    SubscriberKey: String,
    ContactAttributes: ContactAttributesSqsMessage
)

case class ContactAttributesSqsMessage(
    SubscriberAttributes: SubscriberAttributesSqsMessage
)

case class SubscriberAttributesSqsMessage(
    EmailAddress: String,
    edition: String,
    `payment method`: String,
    currency: String,
    amount: String,
    first_name: Option[String],
    date_of_payment: String
)
