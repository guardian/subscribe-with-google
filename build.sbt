
lazy val settings = Seq(
name := """subscribe-with-google""",
organization := "com.gu",
version := "1.0-SNAPSHOT")

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, UniversalPlugin)
  .settings(settings)

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)


riffRaffPackageType := (packageZipTarball in Universal).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
