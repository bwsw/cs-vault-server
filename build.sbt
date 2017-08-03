name := "cs-vault-server"

version := "1.0"

scalaVersion := "2.12.3"

resolvers += Resolver.bintrayRepo("janstenpickle", "maven")

libraryDependencies += "janstenpickle.vault" %% "vault-core" % "0.4.0"
libraryDependencies += "janstenpickle.vault" %% "vault-auth" % "0.4.0"
libraryDependencies += "janstenpickle.vault" %% "vault-manage" % "0.4.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += "org.apache.kafka" % "kafka_2.11" % "0.9.0.1"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.3"
