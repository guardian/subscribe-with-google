package services
import config.Products
import exceptions.UnsupportedSKUException
import model.{SKUCode, SKUType}

import scala.concurrent.Future

trait SKUClient {

  def getSkuType(sku: String): Future[Either[Exception, SKUType]]

}

class SKUClientImpl() extends SKUClient {

  override def getSkuType(sku: String): Future[Either[Exception, SKUType]] = {
    Future.successful(
      Products.skus
        .get(SKUCode(sku))
        .toRight(UnsupportedSKUException(s"SWiG does not currently support the SKU : $sku"))
    )
  }
}
