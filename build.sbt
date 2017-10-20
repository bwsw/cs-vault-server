name := "cs-vault-server"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies += "br.com.autonomiccs" % "apache-cloudstack-java-client" % "1.0.5"
libraryDependencies += "com.bettercloud" % "vault-java-driver" % "3.0.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += ("org.apache.kafka" % "kafka_2.12" % "0.10.1.1")
  .exclude("org.slf4j", "slf4j-api")
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.8.8"
libraryDependencies += "org.apache.zookeeper" % "zookeeper" % "3.4.10"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += ("org.apache.curator" % "curator-recipes" % "2.12.0")
  .exclude("org.slf4j", "slf4j-api")
  .exclude("log4j", "log4j")
  .exclude("io.netty", "netty")
libraryDependencies += "org.apache.curator" % "curator-test" % "2.12.0" % "test"
libraryDependencies += "org.scoverage" %% "scalac-scoverage-runtime" % "1.3.0"
