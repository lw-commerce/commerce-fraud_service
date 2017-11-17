resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// dependency analysis
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// Comment to get more information during initialization
logLevel := Level.Warn

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

// The Typesafe repository
resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Lifeway Repo External Libs" at "http://artifactory.lifeway.org/ext-release-local/"
)