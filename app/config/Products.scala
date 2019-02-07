package config
import model.SKUType.Recurring
import model.{SKUCode, SKUType}

object Products {
  val skus: Map[SKUCode, SKUType] = Map(SKUCode("test_swg_subscription") -> Recurring)
}
