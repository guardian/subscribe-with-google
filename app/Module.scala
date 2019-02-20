import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.AbstractModule
import com.gu.{AppIdentity, AwsIdentity}
import play.api.{Configuration, Environment}
import services._

class Module(env: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[HTTPClient])
      .to(classOf[GoogleHTTPClient])

    bind(classOf[AccessTokenClient])
      .to(classOf[GoogleAccessTokenClient])

    bind(classOf[AmazonCloudWatch])
      .toInstance(AWSClientBuilder.buildCloudWatchAsyncClient())

    val awsCloudWatchAsyncClient = AWSClientBuilder.buildCloudWatchAsyncClient()

    val appName = configuration.get[String]("swg.appName")
    val stage = getEnvironmentStage(appName)

    bind(classOf[MonitoringService])
      .toInstance(new CloudWatchService(awsCloudWatchAsyncClient, stage))
  }

  private def getEnvironmentStage(appName: String): String = {
    AppIdentity.whoAmI(appName) match {
      case AwsIdentity(_, _, stage, _) => stage
      case _ => "DEV"
    }
  }
}
