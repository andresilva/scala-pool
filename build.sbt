name := "scala-pool"

organization := "io.github.andrebeat"
startYear := Some(2015)

version := "0.5.0-SNAPSHOT"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.11", "2.13.1")

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "4.7.1" % "test")

resolvers ++= Seq(
  "snapshots"           at "https://oss.sonatype.org/content/repositories/snapshots",
  "releases"            at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions")

val javaVersion = settingKey[String]("Java version")
javaVersion := System.getProperty("java.version")

unmanagedSourceDirectories in Compile += {
  val v  = javaVersion.value
  val dir = (sourceDirectory in Compile).value

  if (v.startsWith("1.8")) dir / "java_8"
  else dir / "java_7"
}

scalacOptions in Test ++= Seq("-Yrangepos")

fork := true

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging)

pomIncludeRepository := { _ => false }

publishArtifact in Test := false

import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := s"""https://${sys.env.getOrElse("GH_TOKEN", "NULL")}@github.com/andresilva/scala-pool.git"""
