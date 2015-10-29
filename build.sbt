name := "scala-pool"

organization := "io.github.andrebeat"
startYear := Some(2015)
licenses := Seq("MIT License" -> url("https://raw.githubusercontent.com/andrebeat/scala-pool/master/LICENSE"))

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.6.5" % "test")

resolvers ++= Seq(
  "snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"            at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions")

scalacOptions in Test ++= Seq("-Yrangepos")
