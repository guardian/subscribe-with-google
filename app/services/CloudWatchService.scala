package services
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest, PutMetricDataResult}
import model.SKUType
import play.api.Logger._

trait MonitoringService

class CloudWatchService(cloudWatchAsyncClient: AmazonCloudWatchAsync, qualifier: String) extends MonitoringService {
  private val namespace = s"support-subscribe-with-google-$qualifier"

  def put(metricName: String, skuType: SKUType): Unit = {
    val skuTypeDimension = new Dimension()
      .withName("sku-type")
      .withValue(skuType.toString)

    val metric = new MetricDatum()
      .withValue(1d)
      .withMetricName(metricName)
      .withUnit("Count")
      .withDimensions(skuTypeDimension)

    val request = new PutMetricDataRequest()
      .withNamespace(namespace)
      .withMetricData(metric)

    cloudWatchAsyncClient.putMetricDataAsync(request, CloudWatchService.LoggingAsyncHandler)
  }
}

object CloudWatchService {

  private object LoggingAsyncHandler extends AsyncHandler[PutMetricDataRequest, PutMetricDataResult] {

    def onError(exception: Exception): Unit = {
      logger.error("cloud watch service error", exception)
    }

    def onSuccess(request: PutMetricDataRequest, result: PutMetricDataResult): Unit = ()
  }
}
