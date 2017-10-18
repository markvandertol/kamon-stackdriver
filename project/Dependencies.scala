import sbt._

object Dependencies {
  lazy val kamon = "io.kamon" %% "kamon-core" % "1.0.0-RC2-ead4fd7743895ffe1d34e37c23eceab575fb907e" exclude("io.kamon", "kamon-testkit_2.12")
  lazy val specs2 = "org.specs2" %% "specs2-core" % "3.9.5"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  lazy val googleMonitoring = "com.google.cloud" % "google-cloud-monitoring" % "0.25.0-alpha"
  lazy val googleTracing = "com.google.cloud" % "google-cloud-trace" % "0.25.0-alpha"
}
