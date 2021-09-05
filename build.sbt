name := "payment-processing-pipeline"

version := "0.1"

scalaVersion := "2.13.6"
val akkaVersion = "2.6.16"
val akkaCassandraVersion = "1.0.5"
val scalaTestVersion = "3.1.4"
val akkaHttpVersion = "10.2.6"
val alpakkaVersion = "3.0.3"

val kafkaVersion = "2.8.0"

val circeVersion = "0.14.1"

resolvers += Resolver.bintrayRepo("akka", "snapshots")
resolvers ++= Seq (
  Opts.resolver.mavenLocalFile,
  "Confluent" at "https://packages.confluent.io/maven"
)

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

  "com.typesafe" % "config" % "1.4.1",

  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.apache.kafka" % "connect-json" % kafkaVersion,
  "org.apache.kafka" % "connect-runtime" % kafkaVersion,
  "org.apache.kafka" % "kafka-streams" % kafkaVersion,
  "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
  "org.apache.kafka" % "connect-runtime" % kafkaVersion,

  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.4",
  "io.confluent" % "kafka-json-serializer" % "5.0.1",
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" artifacts Artifact("javax.ws.rs-api", "jar", "jar"),

  "io.spray" %%  "spray-json" % "1.3.6",

  "io.circe"  %% "circe-core"     % circeVersion,
  "io.circe"  %% "circe-generic"  % circeVersion,
  "io.circe"  %% "circe-parser"   % circeVersion,

  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  // JWT
  "com.pauldijou" %% "jwt-spray-json" % "5.0.0",

  "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion

)