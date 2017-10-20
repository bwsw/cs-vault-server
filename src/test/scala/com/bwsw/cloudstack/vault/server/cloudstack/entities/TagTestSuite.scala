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
package com.bwsw.cloudstack.vault.server.cloudstack.entities

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import org.scalatest.FlatSpec

class TagTestSuite extends FlatSpec with BaseTestSuite {
  val upperCasePrefix = Tag.Key.prefix.toUpperCase

  "Tag.Key.fromString" should "return Tag.Key.VaultRO" in {
    val testString = "VAULT.RO"
    assert(Tag.Key.fromString(s"$upperCasePrefix$testString") == Tag.Key.VaultRO)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultRW" in {
    val testString = "VAULT.RW"
    assert(Tag.Key.fromString(s"$upperCasePrefix$testString") == Tag.Key.VaultRW)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultHost" in {
    val testString = "VAULT.HOST"
    assert(Tag.Key.fromString(s"$upperCasePrefix$testString") == Tag.Key.VaultHost)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultPrefix" in {
    val testString = "VAULT.PREFIX"
    assert(Tag.Key.fromString(s"$upperCasePrefix$testString") == Tag.Key.VaultPrefix)
  }

  "Tag.Key.toString" should "return vault.ro" in {
    val testString = "vault.ro"
    assert(Tag.Key.toString(Tag.Key.VaultRO) == s"${Tag.Key.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.rw" in {
    val testString = "vault.rw"
    assert(Tag.Key.toString(Tag.Key.VaultRW) == s"${Tag.Key.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.host" in {
    val testString = "vault.host"
    assert(Tag.Key.toString(Tag.Key.VaultHost) == s"${Tag.Key.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.prefix" in {
    val testString = "vault.prefix"
    assert(Tag.Key.toString(Tag.Key.VaultPrefix) == s"${Tag.Key.prefix}$testString")
  }
}
