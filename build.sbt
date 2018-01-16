/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
name := "cs-vault-server"

version := "4.10.3-NP"

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
