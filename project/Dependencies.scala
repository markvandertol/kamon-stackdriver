import sbt._

object Dependencies {
  lazy val kamon = "io.kamon" %% "kamon-core" % "1.1.2"
  lazy val specs2 = "org.specs2" %% "specs2-core" % "3.9.5"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  lazy val googleMonitoring = "com.google.cloud" % "google-cloud-monitoring" % "0.37.0-beta"
  lazy val googleTracing = "com.google.cloud" % "google-cloud-trace" % "0.37.0-beta"
}
