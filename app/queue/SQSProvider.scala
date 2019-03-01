package queue
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Flow, Keep, RestartSource, Sink, Source}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

class SQSProvider()(implicit system: ActorSystem, materializer: Materializer) {
  val config = ConfigFactory.load()


  val queueName = config.getString("sqs.queue-name")
  val queueUrl = config.getString("sqs.queue-url")
  val sqsRegion = config.getString("sqs.region")
  val sqsSecretKey = config.getString("sqs.secret-key")
  val sqsAccessKey = config.getString("sqs.access-key")

  val credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(sqsAccessKey, sqsSecretKey))

  implicit val awsSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withCredentials(credentialsProvider)
    .withEndpointConfiguration(new EndpointConfiguration(queueUrl, sqsRegion))
    .build()

  system.registerOnTermination(awsSqsClient.shutdown())

  //todo: Investigate required custom settings
  val sqsSettings = SqsSourceSettings()

  val sink = SqsAckSink(queueUrl)

  val graph = SqsSource(queueUrl, sqsSettings)
    .via(Flow.fromFunction(handleMessage))
    .toMat(sink)(Keep.right)

  RestartSource.withBackoff(
    3 seconds,
    30 seconds,
    0.2
  ) { () =>
    Source.fromFuture(graph.run())
  }.runWith(Sink.ignore)


  private def handleMessage(message: Message) = {
    message.getBody

    MessageAction.Delete(message)
  }

}
