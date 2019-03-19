package services
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest, PutMetricDataResult}
import model.SKUType
import play.api.Logger._

trait MonitoringService {

  def put(metricName: String, dimensionName: String): Unit

  def put(metricName: String, dimensionName: String, metricValue: String): Unit

  def addSkuTypeCounter(metricName: String, skuType: SKUType): Unit

  def addDeserializationFailure(): Unit

  def addMessageReceived(): Unit

  def addSendSuccessful(): Unit

  def addSendFailure(): Unit

  def addUnsupportedPaymentType(): Unit

  def addUnsupportedNotificationType(): Unit

  def addReceivedTestNotification(): Unit

  def addUnsupportedSKU(): Unit

  def addFailureToGetSubscriptionPurchase(): Unit

  def addUnsupportedPlatformPurchase(): Unit


}

class CloudWatchService(cloudWatchAsyncClient: AmazonCloudWatchAsync, qualifier: String) extends MonitoringService {
  private val namespace = s"support-subscribe-with-google-$qualifier"

  def put(metricName: String, dimensionName: String): Unit = {
    val dimension = new Dimension()
      .withName(dimensionName)
      .withValue(metricName)


    val metric = new MetricDatum()
      .withValue(1d)
      .withMetricName(metricName)
      .withUnit("Count")
      .withDimensions(dimension)

    val request = new PutMetricDataRequest()
      .withNamespace(namespace)
      .withMetricData(metric)

    cloudWatchAsyncClient.putMetricDataAsync(request, CloudWatchService.LoggingAsyncHandler)
  }

  def put(metricName: String, dimensionName: String, metricValue: String): Unit = {
    val skuTypeDimension = new Dimension()
      .withName(dimensionName)
      .withValue(metricValue)

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

  def addSkuTypeCounter(metricName: String, skuType: SKUType): Unit = {
    put(metricName, "sku-type", skuType.toString)
  }

  def addDeserializationFailure(): Unit = {
    put("IncomingPubSubDeserializationFailure", "Retryable")
  }
  override def addMessageReceived(): Unit = put("MessageReceived", "PubSubReceive")
  override def addSendSuccessful(): Unit = put("PaymentAPIRecordSuccess", "Success")
  override def addSendFailure(): Unit = put("PaymentAPIRecordFailure", "Retryable")
  override def addUnsupportedPaymentType(): Unit = put("UnsupportedPaymentType", "IgnorableMessage")
  override def addUnsupportedNotificationType(): Unit = put("UnsupportedNotificationType", "IgnorableMessage")
  override def addReceivedTestNotification(): Unit = put("ReceivedTestNotification", "IgnorableMessage")
  override def addUnsupportedSKU(): Unit = put("UnsupportedSKU", "Retryable")
  override def addFailureToGetSubscriptionPurchase(): Unit = put("FailureToGetSubscriptionPurchase","Retryable")
  override def addUnsupportedPlatformPurchase(): Unit = put("UnsupportedPlatformPurchase", "IgnorableMessage")
}

object CloudWatchService {

  private object LoggingAsyncHandler extends AsyncHandler[PutMetricDataRequest, PutMetricDataResult] {

    def onError(exception: Exception): Unit = {
      logger.error("cloud watch service error", exception)
    }

    def onSuccess(request: PutMetricDataRequest, result: PutMetricDataResult): Unit = ()
  }
}
