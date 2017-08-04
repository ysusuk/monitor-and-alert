name := """monitor-and-alert"""
organization := "com.iuriisusuk"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

resolvers += "Bintary JCenter" at "http://jcenter.bintray.com"

libraryDependencies += guice
libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-MF"
libraryDependencies += "play-circe" %% "play-circe" % "2608.3"
libraryDependencies += "io.circe" %% "circe-generic-extras" % "0.8.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test

resolvers += Resolver.sonatypeRepo("releases")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.iuriisusuk.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.iuriisusuk.binders._"
