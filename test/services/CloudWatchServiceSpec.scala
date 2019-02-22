package services
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import model.SKUType
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.mockito.{Matchers => Match}
import org.mockito.Mockito._

class CloudWatchServiceSpec extends WordSpec with Matchers with ScalaFutures with MockitoSugar {

  val mockAmazonCloudWatchAsync: AmazonCloudWatchAsync = mock[AmazonCloudWatchAsync]

  "CloudWatchService" when {
    "put" in {
      val cloudWatchService = new CloudWatchService(mockAmazonCloudWatchAsync, "TEST")

      cloudWatchService.incrementSkuTypeCounter("metric-name", SKUType.Recurring)

      val skuTypeDimension = new Dimension()
        .withName("sku-type")
        .withValue(SKUType.Recurring.toString)

      val metric = new MetricDatum()
        .withValue(1d)
        .withMetricName("metric-name")
        .withUnit("Count")
        .withDimensions(skuTypeDimension)

      val request = new PutMetricDataRequest()
        .withNamespace("support-subscribe-with-google-TEST")
        .withMetricData(metric)

      verify(mockAmazonCloudWatchAsync, times(1)).putMetricDataAsync(Match.eq(request), Match.any())
    }
  }
}
