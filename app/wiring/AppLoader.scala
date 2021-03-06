package wiring

import play.api.ApplicationLoader.Context
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import com.gu.conf.{ConfigurationLoader, ResourceConfigurationLocation, SSMConfigurationLocation}
import com.typesafe.config.Config
import play.api.Configuration
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}

class AppLoader extends GuiceApplicationLoader {

  override protected def builder(context: Context): GuiceApplicationBuilder = {
    val appName = context.initialConfiguration.get[String]("swg.appName")
    val identity: AppIdentity = AppIdentity.whoAmI(appName)

    val loadedConfig: Config = ConfigurationLoader.load(identity) {
      case AwsIdentity(app, stack, stage, _) => SSMConfigurationLocation(s"/$app/$stage")
      case DevIdentity(app)                  => ResourceConfigurationLocation("application.dev.conf")
    }

    val builder: GuiceApplicationBuilder = initialBuilder.in(context.environment).overrides(overrides(context): _*)
    val configuration = context.initialConfiguration ++ Configuration(loadedConfig)

    builder.loadConfig(configuration)
  }
}
