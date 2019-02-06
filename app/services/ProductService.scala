package services

import exceptions.DeserializationException
import javax.inject._
import play.api.libs.json.Json
import play.api.Environment
import model.Product

trait ConfigService

@Singleton
class ProductService @Inject()(env: Environment) extends ConfigService {

  val products: List[Product] = loadProductsFromJson("products.json")

  def loadProductsFromJson(file: String): List[Product] = {
    val productsJsonStream = env.classLoader.getResourceAsStream(file)
    Json.parse(productsJsonStream).validate[List[Product]].asEither match {
      case Left(l) =>
        throw DeserializationException("Error deserialising products JSON", l)
      case Right(r) => r
    }
  }

  def get(sku: String): Option[Product] = products.find(_.sku == sku)
}
