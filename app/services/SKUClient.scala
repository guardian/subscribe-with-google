package services
import model.SKUType

import scala.concurrent.Future

trait SKUClient {

  def getSkuType(sku: String): Future[Either[Exception, SKUType]]

}