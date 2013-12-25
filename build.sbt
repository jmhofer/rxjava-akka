organization := "de.johoop"

name := "rxjava-akka"

version := "1.0.0"

scalaVersion := "2.10.3"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
	"com.netflix.rxjava" % "rxjava-core" % "0.15.1",
	"com.netflix.rxjava" % "rxjava-scala" % "0.15.1",
	"com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "org.specs2" %% "specs2" % "2.3.6" % "test")

scalacOptions ++= Seq("-language:_", "-deprecation")