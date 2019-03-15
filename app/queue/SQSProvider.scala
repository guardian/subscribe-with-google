package queue

import cats.implicits._
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Flow, Keep, RestartSource, Sink, Source}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.{Message, QueueDoesNotExistException}
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import config.CredentialProvider
import exceptions.{DeserializationException, IgnorableException}
import javax.inject.Inject
import javax.inject.Singleton
import model.GooglePushMessageWrapper
import play.api.Configuration
import play.api.Logger._
import play.api.libs.json.Json
import routing.MessageRouter

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

trait SQSListener

@Singleton
class SQSListenerImpl @Inject()(messageRouter: MessageRouter,
                                credentialProvider: CredentialProvider,
                                config: Configuration)(implicit system: ActorSystem, materializer: Materializer)
    extends SQSListener {

  private implicit val ec = system.dispatcher

  private val queueUrl = config.get[String]("sqs.queue-url")
  private val sqsRegion = config.get[String]("sqs.region")

  logger.info(s"Starting up SQS Consumer for : $queueUrl")

  implicit val awsSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withCredentials(credentialProvider.credentialProvider)
    .withEndpointConfiguration(new EndpointConfiguration(queueUrl, sqsRegion))
    .build()

  system.registerOnTermination(awsSqsClient.shutdown())

  queueExists(queueUrl)

  //todo: Investigate required custom settings
  val sqsSettings = SqsSourceSettings()
  val sink = SqsAckSink(queueUrl)

  val graph = SqsSource(queueUrl, sqsSettings)
    .map(f => {
      logger.debug(s"Message received :: ${f.getBody}")
      f
    })
    .via(Flow.fromFunction(handleMessage))
    .mapAsync(1)(f => f)
    .toMat(sink)(Keep.right)

  RestartSource
    .withBackoff(
      3 seconds,
      30 seconds,
      0.2
    ) { () =>
      logger.info("Starting up SQS Stream")
      Source.fromFuture(graph.run())
    }
    .runWith(Sink.ignore)

  private def handleMessage(message: Message): Future[MessageAction] = {
    val parseMessage = () => {
      Json.parse(message.getBody)
        .validate[GooglePushMessageWrapper]
        .asEither
        .leftMap { errs =>
          DeserializationException(s"Unable to deserialize GooglePushMessage :: ${message.getBody}", errs)
        }
    }

    messageRouter.handleMessage(parseMessage).map{
      case Left(e: IgnorableException) =>
        logger.info(s"Ignorable exception encountered ${e.getMessage}")
        MessageAction.Delete(message)
      case Left(e) =>
        logger.error("Failure to complete payment :: This is a recoverable error - placing message back on queue", e)
        MessageAction.Ignore(message)
      case Right(_) =>
        MessageAction.Delete(message)
    }
  }

  private def queueExists(queueUrl: String): Unit = {
    try {
      awsSqsClient.getQueueAttributes(queueUrl, Seq("All").asJava)
      logger.info(s"Queue at $queueUrl found.")
    } catch {
      case queueDoesNotExistException: QueueDoesNotExistException =>
        logger.error(s"The queue with url $queueUrl does not exist.")
        throw queueDoesNotExistException
    }
  }
}
