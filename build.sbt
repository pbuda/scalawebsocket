name := "scalawebsocket"

homepage := Some(url("https://github.com/pbuda/scalawebsocket"))

licenses := Seq("Apache License 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

organization := "eu.piotrbuda"

version := "0.1.2"

scalaVersion := "2.11.6"

crossScalaVersions := List("2.11.6", "2.10.5")

fork in Test := true

libraryDependencies ++= Seq(
  "com.ning" % "async-http-client" % "1.7.13",

  //logging
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback"             %  "logback-classic"     % "1.1.3",

  //jetty is used to setup test server
  "org.eclipse.jetty" % "jetty-server"    % "8.1.7.v20120910" % "test",
  "org.eclipse.jetty" % "jetty-websocket" % "8.1.7.v20120910" % "test",
  "org.eclipse.jetty" % "jetty-servlet"   % "8.1.7.v20120910" % "test",
  "org.eclipse.jetty" % "jetty-servlets"  % "8.1.7.v20120910" % "test",

  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

publishMavenStyle := true

publishTo <<= version {
  (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := {
  _ => false
}

pomExtra := (
  <scm>
    <url>git@github.com:pbuda/scalawebsocket.git</url>
    <connection>scm:git:git@github.com:pbuda/scalawebsocket.git</connection>
    </scm>
    <developers>
    <developer>
    <id>pbuda</id>
    <name>Piotr Buda</name>
    <url>http://www.piotrbuda.eu</url>
      </developer>
    </developers>
)
