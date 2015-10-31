name := "scala-pool"

organization := "io.github.andrebeat"
startYear := Some(2015)
licenses := Seq("MIT License" -> url("https://raw.githubusercontent.com/andrebeat/scala-pool/master/LICENSE"))

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.6.5" % "test")

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

scalacOptions in Test ++= Seq("-Yrangepos")

fork := true

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/andrebeat/scala-pool</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>https://raw.githubusercontent.com/andrebeat/scala-pool/master/LICENSE</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:andrebeat/scala-pool.git</url>
    <connection>scm:git:git@github.com:andrebeat/scala-pool.git</connection>
  </scm>
  <developers>
    <developer>
      <id>andrebeat</id>
      <name>Andre Silva</name>
      <url>https://github.com/andrebeat/</url>
    </developer>
  </developers>
)

import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)

site.settings
site.includeScaladoc()
ghpages.settings
git.remoteRepo := s"""https://${sys.env.getOrElse("GH_TOKEN", "NULL")}@github.com/andrebeat/scala-pool.git"""
