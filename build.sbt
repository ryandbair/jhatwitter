name := "jhatwitter"

version := "0.1"

scalaVersion := "2.13.1"

val circeVersion = "0.12.3"
val akkaVersion = "2.6.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "de.heikoseeberger" %% "akka-http-circe" % "1.29.1",
  "com.vdurmont" % "emoji-java" % "5.1.1",
  "com.iheart" %% "ficus" % "1.4.7",
) ++ Seq(
  "com.typesafe.akka" %% "akka-actor",
  "com.typesafe.akka" %% "akka-actor-typed",
  "com.typesafe.akka" %% "akka-stream",
  "com.typesafe.akka" %% "akka-stream-typed",
).map(_ % akkaVersion) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

dependsOn(
  ProjectRef(uri("git://github.com/ryandbair/koauth.git#master"), "koauth"),
  ProjectRef(uri("git://github.com/hseeberger/akka-http-json.git"), "akka-http-circe"), // Streaming JSON support isn't in a current release :(
)