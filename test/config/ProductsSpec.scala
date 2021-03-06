package config

import model.SKUCode
import model.SKUType.{Recurring, Single}
import org.scalatest.{Matchers, WordSpecLike}

class ProductsSpec extends WordSpecLike with Matchers {

  "Products" must {
    "Retrieve SKU type" in {
      Products.skus.get(SKUCode("test_swg_subscription")) shouldBe Some(Single)
    }

    "Unknown SKU not found" in {
      Products.skus.get(SKUCode("unknown_sku")) shouldBe None
    }
  }
}
