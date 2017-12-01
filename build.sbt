name := "scala-pool"

organization := "io.github.andrebeat"
startYear := Some(2015)
licenses := Seq("MIT License" -> url("https://raw.githubusercontent.com/andresilva/scala-pool/master/LICENSE"))

version := "0.5.0-SNAPSHOT"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.11", "2.10.6")

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.9.5" % "test")

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

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("staging"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

publishArtifact in Test := false

pomExtra := (
  <url>https://github.com/andresilva/scala-pool</url>
  <scm>
    <url>git@github.com:andresilva/scala-pool.git</url>
    <connection>scm:git:git@github.com:andresilva/scala-pool.git</connection>
  </scm>
  <developers>
    <developer>
      <id>andresilva</id>
      <name>Andre Silva</name>
      <url>https://github.com/andresilva/</url>
    </developer>
  </developers>
)

import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := s"""https://${sys.env.getOrElse("GH_TOKEN", "NULL")}@github.com/andresilva/scala-pool.git"""
