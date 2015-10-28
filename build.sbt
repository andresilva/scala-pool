name := "scala-pool"

organization := "io.github.andrebeat"
startYear := Some(2015)
licenses := Seq("MIT License" -> url("https://raw.githubusercontent.com/andrebeat/scala-pool/master/LICENSE"))

version := "0.1"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"            at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature")
