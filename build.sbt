import com.typesafe.sbt.packager.archetypes.systemloader.ServerLoader.Systemd
import PlayKeys._

lazy val settings = Seq(
name := """subscribe-with-google""",
organization := "com.gu",
version := "1.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging)
  .settings(settings)
    .settings(PlayKeys.playDefaultPort := 9233)

scalaVersion := "2.12.8"

scapegoatVersion in ThisBuild := "1.3.2"

val enumeratumPlayJsonVersion = "1.5.16"
val AWSJavaSDKVersion = "1.11.501"

resolvers += "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms"

libraryDependencies ++= Seq(
  guice,
  "com.beachape" %% "enumeratum-play-json" % enumeratumPlayJsonVersion,
  ws,
  caffeine,
  "com.github.blemale" %% "scaffeine" % "2.5.0" % "compile",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % AWSJavaSDKVersion,
  "com.gu" %% "simple-configuration-ssm" % "1.4.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "de.leanovate.play-mockws" %% "play-mockws" % "2.7.0" % Test
)

topLevelDirectory in Universal := None
packageName in Universal := normalizedName.value

riffRaffPackageType := (packageBin in Debian).value
riffRaffManifestProjectName := "support:subscribe-with-google"
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), "subscribe-with-google-cloudformation/cfn.yaml")
serverLoading in Debian := Option(Systemd)
maintainer := "Contribute with Google"
packageSummary := "Contribute with Google"
packageDescription := """Contribute with Google service - allows for integration with subscribe and contribute with google"""


coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"
scapegoatIgnoredFiles := Seq(".*Reverse.*", ".*router.*")

scapegoatDisabledInspections := Seq("FinalModifierOnCaseClass")

javaOptions in Universal ++= Seq(
  "-Dhttp.port=9233",
)
