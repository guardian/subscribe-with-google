package config

import com.amazonaws.auth._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import play.api.Configuration

trait CredentialProvider {

  def credentialProvider: AWSCredentialsProvider

}



class CredentialProviderImpl(stage: String, configuration: Configuration) extends CredentialProvider {

  override def credentialProvider: AWSCredentialsProvider = {
    if (stage == "DEV") {
      val sqsSecretKey = configuration.get[String]("sqs.secret-key")
      val sqsAccessKey = configuration.get[String]("sqs.access-key")

      new AWSStaticCredentialsProvider(new BasicAWSCredentials(sqsAccessKey, sqsSecretKey))
    } else {
      new AWSCredentialsProviderChain(
        new ProfileCredentialsProvider("membership"),
        new InstanceProfileCredentialsProvider(false))
    }
  }
}
