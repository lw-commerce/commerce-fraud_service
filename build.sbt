import sbtbuildinfo.BuildInfoPlugin.autoImport._
import scoverage.ScoverageKeys._

val buildVersion = Option(System.getenv("BUILD_VERSION")).getOrElse("DEV")

name := """fraudservice"""

version := buildVersion

scalacOptions ++= Seq("-feature", "-target:jvm-1.8")

scalaVersion := "2.11.11"

val playVersion = "2.6.5"

libraryDependencies ++= Seq(
  // logging
  "org.slf4j" % "slf4j-api" % "1.7.25",

  "com.typesafe.play" %% "play-logback" % playVersion,
  "org.apache.kafka" %% "kafka" % "0.10.2.1",
  "com.typesafe.akka" % "akka-stream-kafka_2.11" % "0.17",
  "org.mongodb" % "mongo-java-driver" % "3.2.2",
  "org.mongodb" %% "casbah" % "3.1.1",

  // metrics endpoint
  filters,
  "com.lifeway.it" %% "prometheus-client-play_2-6" % "1.6.5",
  "org.json4s" % "json4s-native_2.11" % "3.5.3",
  ws,
  guice,

  // test
  "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % Test,
  specs2 % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test excludeAll(ExclusionRule(organization = "org.seleniumhq.selenium"))

)

resolvers ++= Seq(
  "Java.net Maven2 Repository" at "http://download.java.net/maven/2/",
  //"Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  "Lifeway Repo External Libs" at "http://artifactory.lifeway.org/ext-release-local/",
  "Lifeway Repo Internal Libs" at "http://artifactory.lifeway.org/libs-release-local/",
  "Lifeway Repo Internal SNAPSHOTS" at "http://artifactory.lifeway.org/libs-snapshot-local/"
)

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
//routesGenerator := InjectedRoutesGenerator

parallelExecution in Test := false

fork in Test := false

javaOptions in Test += "-Dconfig.file=" + Option(System.getProperty("config.file")).getOrElse("conf/application.conf")

//----------------------------------------
// Configuration for Scoverage Scala Code Coverage Tool
// Invocation: coverage test
// Results: target/scala-x.xx/scoverage-report/index.html
// See: https://github.com/scoverage/sbt-scoverage
//-----------------------------------------
val scalaLinePercent = 90

coverageMinimum := scalaLinePercent

coverageFailOnMinimum := true

coverageExcludedPackages := "<empty>;Reverse*;.*AWSCluster.*;.*ApplicationComponents.*;.*Routes.*"
coverageExcludedPackages := "<empty>;controllers\\.routes.*;controllers\\.Reverse.*;controllers\\.javascript.*;controllers\\.ref.*;.*domain.*;.*values.*;.*util.*;.*router.*;.*views.html.*;.*annotation.*"

coverageExcludedFiles := ".*DomainEvents.*;.*GuiceInjectionModule.*;.*HealthBuilder.*;.*HealthCheckService.*;.*AppInfo.*;.*BuildInfo.*;.*Value.*;.*RuleGroup.*"

coverageEnabled.in(Test, test) := true

val appVersion = "1.0"

lazy val fraudapi = (project in file(".")).enablePlugins(PlayScala, BuildInfoPlugin)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.lifeway.it.order.fraud.build",
      buildInfoOptions += BuildInfoOption.ToJson
    )

buildInfoKeys ++= Seq[BuildInfoKey](
  BuildInfoKey.action("gitCommit") {
    Option(System.getenv("GIT_COMMIT")).getOrElse("")
  },
  BuildInfoKey.action("buildTime") {
    if(buildVersion != "DEV") System.currentTimeMillis else ""
  },
  BuildInfoKey.action("appVersion") {
    appVersion
  }
)
