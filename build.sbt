import de.heikoseeberger.sbtheader.license.Apache2_0
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import com.typesafe.sbt.SbtGit._

name := "StockFighter"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

headers := Map(
  "scala" -> Apache2_0("2016", "Pere Villega"),
  "conf" -> Apache2_0("2016", "Pere Villega", "#")
)

enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.bintrayRepo("projectseptemberinc", "maven")

val catsVersion = "0.7.2"
val pureConfigVersion = "0.2.1"
val monixVersion = "2.0.0"
val circeVersion = "0.5.1"
val scalaCheckVersion = "1.13.0"
val scalaTestVersion = "2.2.6"
val kindProjectorVersion = "0.8.1"
val si2712fixVersion = "1.2.0"
val freekVersion = "0.6.1"
val nscalaVersion = "2.12.0"
val gigaHorseVersion = "0.1.1"

libraryDependencies ++= Seq(
  "org.spire-math" %% "kind-projector" % kindProjectorVersion,
  "com.milessabin" % "si2712fix-plugin" % si2712fixVersion cross CrossVersion.full,
  "com.projectseptember" %% "freek" % freekVersion,
  "com.github.melrief" %% "pureconfig" % pureConfigVersion,
  "org.typelevel" %% "cats" % catsVersion,
  "com.eed3si9n" %% "gigahorse-core" % gigaHorseVersion,
  "io.monix" %% "monix" % monixVersion,
  "io.monix" %% "monix-cats" % monixVersion,
  "com.github.nscala-time" %% "nscala-time" % nscalaVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test",
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % kindProjectorVersion)
addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % si2712fixVersion cross CrossVersion.full)
