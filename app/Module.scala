import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.AbstractModule
import com.gu.{AppIdentity, AwsIdentity}
import config.{CredentialProvider, CredentialProviderImpl}
import play.api.{Configuration, Environment}
import queue.{SQSListener, SQSListenerImpl}
import routing.{MessageRouter, MessageRouterImpl}
import services._

class Module(env: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    val appName = configuration.get[String]("swg.appName")
    val stage = getEnvironmentStage(appName)


    bind(classOf[HTTPClient])
      .to(classOf[GoogleHTTPClient])

    bind(classOf[AccessTokenClient])
      .to(classOf[GoogleAccessTokenClient])

    bind(classOf[MessageRouter])
      .to(classOf[MessageRouterImpl])

    bind(classOf[SKUClient])
        .to(classOf[SKUClientImpl])

    bind(classOf[CredentialProvider])
        .toInstance(new CredentialProviderImpl(stage))

    bind(classOf[SQSListener])
      .to(classOf[SQSListenerImpl]).asEagerSingleton()

    bind(classOf[AmazonCloudWatch])
      .toInstance(AWSClientBuilder.buildCloudWatchAsyncClient())

    val awsCloudWatchAsyncClient = AWSClientBuilder.buildCloudWatchAsyncClient()

    bind(classOf[MonitoringService])
      .toInstance(new CloudWatchService(awsCloudWatchAsyncClient, stage))
  }

  private def getEnvironmentStage(appName: String): String = {
    AppIdentity.whoAmI(appName) match {
      case AwsIdentity(_, _, stage, _) => stage
      case _                           => "DEV"
    }
  }
}
