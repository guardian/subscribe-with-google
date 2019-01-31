import com.typesafe.sbt.packager.archetypes.systemloader.ServerLoader.Systemd
import PlayKeys._

lazy val settings = Seq(
name := """subscribe-with-google""",
organization := "com.gu",
version := "1.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging)
  .settings(settings)

scalaVersion := "2.12.8"

scapegoatVersion in ThisBuild := "1.3.2"

val enumeratumPlayJsonVersion = "1.5.14"

libraryDependencies ++= Seq(
  guice,
  "com.beachape" %% "enumeratum-play-json" % enumeratumPlayJsonVersion,
  ws,
  ehcache,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.6.2" % Test
)

topLevelDirectory in Universal := None
packageName in Universal := normalizedName.value

riffRaffPackageType := (packageBin in Debian).value
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
