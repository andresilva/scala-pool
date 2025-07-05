name := "scala-pool"

organization := "io.github.andrebeat"
startYear    := Some(2015)

version := "0.5.0-SNAPSHOT"

scalaVersion := "2.13.16"

crossScalaVersions := Seq("2.13.16", "2.12.20")

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

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

pomIncludeRepository := { _ => false }

Test / publishArtifact := false

scalafmtOnCompile := true

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := {
  val actor = sys.env.getOrElse("GITHUB_ACTOR", "NULL")
  val token = sys.env.getOrElse("GITHUB_TOKEN", "NULL")

  s"""https://$actor:$token@github.com/andresilva/scala-pool.git"""
}
