package config

import com.amazonaws.auth._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.typesafe.config.ConfigFactory

trait CredentialProvider {

  def credentialProvider: AWSCredentialsProvider

}



class CredentialProviderImpl(stage: String) extends CredentialProvider {

  override def credentialProvider: AWSCredentialsProvider = {
    if (stage == "DEV") {
      val config = ConfigFactory.load()

      val queueUrl = config.getString("sqs.queue-url")
      val sqsRegion = config.getString("sqs.region")
      val sqsSecretKey = config.getString("sqs.secret-key")
      val sqsAccessKey = config.getString("sqs.access-key")

      new AWSStaticCredentialsProvider(new BasicAWSCredentials(sqsAccessKey, sqsSecretKey))
    } else {
      new AWSCredentialsProviderChain(
        new ProfileCredentialsProvider("membership"),
        new InstanceProfileCredentialsProvider(false))
    }
  }
}
