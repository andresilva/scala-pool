name := "scala-pool"

organization := "io.github.andrebeat"
startYear    := Some(2015)

scalaVersion := "3.3.6"

crossScalaVersions := Seq("3.3.6", "2.13.16", "2.12.20")

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "4.21.0" % "test")

resolvers ++= Seq(
  "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions"
)

Test / scalacOptions ++= Seq("-Yrangepos")

fork := true

publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

pomIncludeRepository := { _ => false }

Test / publishArtifact := false

releaseCrossBuild    := true
releaseTagComment    := s"Release ${(ThisBuild / version).value}"
releaseCommitMessage := s"Set version to ${(ThisBuild / version).value}"

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonaRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

scalafmtOnCompile := true

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := {
  val actor = sys.env.getOrElse("GITHUB_ACTOR", "NULL")
  val token = sys.env.getOrElse("GITHUB_TOKEN", "NULL")

  s"""https://$actor:$token@github.com/andresilva/scala-pool.git"""
}
