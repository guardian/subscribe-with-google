
lazy val settings = Seq(
name := """subscribe-with-google""",
organization := "com.gu",
version := "1.0"
PlayKeys.playDefaultPort := 9233
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging)
  .settings(settings)

scalaVersion := "2.12.8"

scapegoatVersion in ThisBuild := "1.3.2"

val enumeratumPlayJsonVersion = "1.5.14"

libraryDependencies ++= Seq(
  guice,
  "com.beachape" %% "enumeratum-play-json" % enumeratumPlayJsonVersion,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

topLevelDirectory in Universal := None
packageName in Universal := normalizedName.value

riffRaffPackageType := (packageBin in Debian).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")
//add .deb to artifcat resources

coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"
scapegoatIgnoredFiles := Seq(".*Reverse.*", ".*router.*")

scapegoatDisabledInspections := Seq("FinalModifierOnCaseClass")
