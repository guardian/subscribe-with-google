package queue
import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.sqs.SqsPublishResult
import akka.stream.alpakka.sqs.scaladsl.{SqsPublishFlow, SqsPublishSink}
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model.SendMessageRequest
import config.CredentialProvider
import javax.inject.{Inject, Singleton}
import model.email.ContributorRow
import play.api.Configuration

import scala.concurrent.Future

@Singleton
class EmailSQSQueue @Inject()(credentialProvider: CredentialProvider, config: Configuration)
                             (implicit system: ActorSystem, materializer: Materializer) {

  private val queueUrl = config.get[String]("email.sqs.queue-url")

  private val sqsRegion = config.get[String]("email.sqs.region")

  private implicit val awsSqsAsyncClient = AmazonSQSAsyncClientBuilder
    .standard()
    .withCredentials(credentialProvider.credentialProvider)
    .withEndpointConfiguration(new EndpointConfiguration(queueUrl, sqsRegion))
    .build()



  def sendMessage(contributorRow: ContributorRow): Future[SqsPublishResult] = {
    Source
      .single(new SendMessageRequest(queueUrl, contributorRow.toJsonContributorRowSqsMessage))
      .via(SqsPublishFlow())
      .runWith(Sink.head)
  }


}
