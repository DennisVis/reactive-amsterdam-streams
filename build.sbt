name := "reactive-amsterdam-streams"

version := "1.0"

scalaVersion := "2.12.1"

resolvers ++= Seq(
  Resolver.bintrayRepo("cakesolutions", "maven")
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.17",
  "com.typesafe.akka" %% "akka-http" % "10.0.1",
  "net.cakesolutions" %% "scala-kafka-client-testkit" % "0.10.2.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.12",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.14"
)
