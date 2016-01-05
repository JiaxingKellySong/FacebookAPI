
name := "Facebook"

version := "1.0"

resolvers += "spray repo" at "http://repo.spray.io"

scalaVersion := "2.11.7"

val sprayVersion = "1.3.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.13"

libraryDependencies += "io.spray" %% "spray-can" % sprayVersion

libraryDependencies += "io.spray" %% "spray-routing" % sprayVersion

libraryDependencies += "io.spray" %% "spray-client" % sprayVersion

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.10"

libraryDependencies += "io.spray" %% "spray-json"   % sprayVersion