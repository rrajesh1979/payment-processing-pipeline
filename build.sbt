name := "payment-processing-pipeline"

version := "0.1"

scalaVersion := "2.13.6"
val akkaVersion = "2.6.16"

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-persistence
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,

  // https://mvnrepository.com/artifact/com.github.scullxbones/akka-persistence-mongo-rxmongo
  "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "3.0.6",

  // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
  "com.google.protobuf" % "protobuf-java" % "3.17.3"


)