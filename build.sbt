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
lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "cs-vault-server",
    organization := "com.bwsw",
    version := "4.10.3-NP",
    scalaVersion := "2.12.4",
    libraryDependencies ++= Seq(
      "br.com.autonomiccs" % "apache-cloudstack-java-client" % "1.0.5",
      "com.bettercloud" % "vault-java-driver" % "3.0.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8",
      "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.8.8",
      "com.google.guava" % "guava" % "23.0",
      "com.typesafe" % "config" % "1.3.0",
      ("org.apache.curator" % "curator-recipes" % "4.0.0")
        .exclude("org.slf4j", "slf4j-api")
        .exclude("log4j", "log4j")
        .exclude("io.netty", "netty")
        .exclude("com.google.guava", "guava"),
      ("org.apache.curator" % "curator-test" % "4.0.0" % "test")
        .exclude("com.google.guava", "guava"),
      ("org.apache.kafka" % "kafka_2.12" % "0.10.1.1")
        .exclude("org.slf4j", "slf4j-api"),
      "org.apache.zookeeper" % "zookeeper" % "3.4.10",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.scoverage" %% "scalac-scoverage-runtime" % "1.3.0",
      "org.slf4j" % "slf4j-api" % "1.7.25"
    ),
    addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17"),
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/bwsw/cs-vault-server")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/bwsw/cs-vault-server"),
        "scm:git@github.com:bwsw/cs-vault-server.git"
      )
    ),
    developers := List(
      Developer(
        id = "bitworks",
        name = "Bitworks Software, Ltd.",
        email = "bitworks@bw-sw.com",
        url = url("http://bitworks.software/")
      )
    ),
    inConfig(IntegrationTest)(Defaults.itSettings),
    coverageEnabled in Test := true,
    inConfig(IntegrationTest)(ScalastylePlugin.rawScalastyleSettings()) ++
      Seq(
        scalastyleConfig in IntegrationTest := (scalastyleConfig in scalastyle).value,
        scalastyleTarget in IntegrationTest := target.value / "scalastyle-it-results.xml",
        scalastyleFailOnError in IntegrationTest := (scalastyleFailOnError in scalastyle).value,
        (scalastyleFailOnWarning in IntegrationTest) := (scalastyleFailOnWarning in scalastyle).value,
        scalastyleSources in IntegrationTest := (unmanagedSourceDirectories in IntegrationTest).value
      )
  )
