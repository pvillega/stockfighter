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

wartremoverErrors ++= Warts.unsafe

enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)

