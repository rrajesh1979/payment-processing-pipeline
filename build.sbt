name := "payment-processing-pipeline"

version := "0.1"

scalaVersion := "2.13.6"
val akkaVersion = "2.6.16"
val akkaCassandraVersion = "1.0.5"

resolvers += Resolver.bintrayRepo("akka", "snapshots")

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-persistence
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,

  "com.typesafe.akka" %% "akka-coordination" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,

  // https://mvnrepository.com/artifact/com.github.scullxbones/akka-persistence-mongo-rxmongo
  "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "3.0.6",

  // https://mvnrepository.com/artifact/org.postgresql/postgresql
  "org.postgresql" % "postgresql" % "42.2.23",
  // https://mvnrepository.com/artifact/com.github.dnvriend/akka-persistence-jdbc
  "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.5.3",

  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-persistence-cassandra
  "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaCassandraVersion,
  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-persistence-cassandra-launcher
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % akkaCassandraVersion % Test,


// https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
  "com.google.protobuf" % "protobuf-java" % "3.17.3",

  "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.0",

)