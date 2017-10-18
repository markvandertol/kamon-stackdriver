import Dependencies._
import scalariform.formatter.preferences._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "nl.markvandertol",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Kamon-Stackdriver",
    crossScalaVersions := Seq("2.12.3", "2.11.11", "2.10.6"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= List(
      specs2 % Test,
      kamon,
      googleMonitoring,
      googleTracing,
      logback),
    scalariformPreferences := scalariformPreferences.value
  )
