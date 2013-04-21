import sbt._

object SWSPlugins extends Build {
  val plugins = Project("SWSPlugins", file("."))
    .settings(addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.3.0"))
    .settings(addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8"))
}