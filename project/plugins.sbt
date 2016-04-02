logLevel := Level.Warn

resolvers += Resolver.typesafeRepo("releases")

//Automatic formatting of scala code on compilation. See https://github.com/sbt/sbt-scalariform
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// Shows dependencies that can be updated. See https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.10")

// Code linting to detect common errors. See https://github.com/puffnfresh/wartremover
addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.14")

// Shows dependency graph of the project. See https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// Facilitates cleaning local Ivy cache for when you use 'publishLocal' often. See https://github.com/sbt/sbt-dirty-money
addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

// Add license headers to code files. See https://github.com/sbt/sbt-header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")

// Offers git command line features directly inside sbt. See https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
