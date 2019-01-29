
lazy val settings = Seq(
name := """subscribe-with-google""",
organization := "com.gu",
version := "1.0-SNAPSHOT",
PlayKeys.playDefaultPort := 9233
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, UniversalPlugin)
  .settings(settings)

scalaVersion := "2.12.8"

scapegoatVersion in ThisBuild := "1.3.2"

val enumeratumPlayJsonVersion = "1.5.14"

libraryDependencies ++= Seq(
  guice,
  "com.beachape" %% "enumeratum-play-json" % enumeratumPlayJsonVersion,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

enablePlugins(JavaAppPackaging, RiffRaffArtifact)

topLevelDirectory in Universal := None
packageName in Universal := s"support:${normalizedName.value}"

riffRaffPackageType := (packageBin in Universal).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")


coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"
scapegoatIgnoredFiles := Seq(".*Reverse.*", ".*router.*")

scapegoatDisabledInspections := Seq("FinalModifierOnCaseClass")
