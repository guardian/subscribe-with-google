package model
import play.api.libs.json.Json

case class Product(sku: String, name: String, productType: ProductType)

object Product {
  implicit val format = Json.format[Product]
}
