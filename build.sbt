name := """short-url"""

version := "1.0"

scalaVersion := "2.11.2"


// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "io.spray" %% "spray-can" % "1.3.1" % "compile",
  "io.spray" %% "spray-routing" % "1.3.1" % "compile",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "net.debasishg" %% "redisclient" % "2.13",
  "com.livestream" %% "scredis" % "2.0.0",
  "com.storm-enroute" %% "scalameter" % "0.6" %"test"
)

