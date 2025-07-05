addSbtPlugin("org.scoverage"    % "sbt-scoverage" % "2.3.1")
addSbtPlugin("org.scoverage"    % "sbt-coveralls" % "1.3.15")
addSbtPlugin("com.typesafe.sbt" % "sbt-site"      % "1.3.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages"   % "0.6.3")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"  % "2.5.5")
addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype"  % "2.3")
addSbtPlugin("com.jsuereth"     % "sbt-pgp"       % "2.0.0")

// taken from https://github.com/scala/bug/issues/12632
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
