import com.google.inject.AbstractModule
import services.{HTTPClient, SKULookup}

class Module extends AbstractModule {
  def configure() = {
      bind(classOf[HTTPClient])
        .to(classOf[SKULookup])
    }
}
