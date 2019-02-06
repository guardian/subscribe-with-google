package services
import java.io.{ByteArrayInputStream, File, InputStream}

import model.Product
import model.ProductType.Recurring
import org.scalatest.{Matchers, WordSpecLike}
import play.api.{Environment, Mode}

class ProductServiceSpec extends WordSpecLike with Matchers {

  "ProductService" must {
    "retrieve and deserialise Product by SKU" in {
      val json =
        s"""[
           |{
           |  "sku": "test_swg_subscription",
           |  "name": "Test SwG Subscription",
           |  "productType": "recurring"
           |}
           |]
         """.stripMargin

      class MockClassLoader extends ClassLoader {
        override def getResourceAsStream(file: String): InputStream = {
          new ByteArrayInputStream(json.getBytes)
        }
      }

      val productService = new ProductService(
        Environment(new File("path/to/app"), new MockClassLoader(), Mode.Test)
      )

      productService.get("test_swg_subscription") shouldBe Some(Product(
        "test_swg_subscription",
        "Test SwG Subscription",
        Recurring
      ))
    }

    "retrieval returns None if product not found" in {
      val json =
        s"""[
           |{
           |  "sku": "test_swg_subscription",
           |  "name": "Test SwG Subscription",
           |  "productType": "recurring"
           |}
           |]
         """.stripMargin

      class MockClassLoader extends ClassLoader {
        override def getResourceAsStream(file: String): InputStream = {
          new ByteArrayInputStream(json.getBytes)
        }
      }

      val productService = new ProductService(
        Environment(new File("path/to/app"), new MockClassLoader(), Mode.Test)
      )

      productService.get("someInvalidSkuCode") shouldBe None
    }
  }
}
