name := "cs-vault-server"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies += "br.com.autonomiccs" % "apache-cloudstack-java-client" % "1.0.5"
libraryDependencies += "com.bettercloud" % "vault-java-driver" % "2.0.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += ("org.apache.kafka" % "kafka_2.12" % "0.10.1.1")
  .exclude("org.slf4j", "slf4j-api")
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.8.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "org.apache.zookeeper" % "zookeeper" % "3.4.10"
