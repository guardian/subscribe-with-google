package model

import play.api.libs.json.Json

case class Price(priceMicros: String, currency: String)

case class Listing(title: String, description: String)

case class SeasonDate(month: Int, day: Int)

case class Proration(start: SeasonDate, defaultPrice: Price)

case class Season(start: SeasonDate,
                  end: SeasonDate,
                  prorations: Option[List[Proration]])

case class SKU(packageName: String,
               sku: String,
               status: String,
               purchaseType: String,
               defaultPrice: Price,
               prices: Map[String, Price],
               listings: Map[String, Listing],
               defaultLanguage: String,
               subscriptionPeriod: String,
               season: Season,
               trialPeriod: String)

object Price {
  implicit val format = Json.format[Price]
}

object Listing {
  implicit val format = Json.format[Listing]
}

object SeasonDate {
  implicit val format = Json.format[SeasonDate]
}

object Proration {
  implicit val format = Json.format[Proration]
}

object Season {
  implicit val format = Json.format[Season]
}

object SKU {
  implicit val format = Json.format[SKU]
}
