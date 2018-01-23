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

import org.scalatest.FlatSpec

class VaultTagKeyTestSuite extends FlatSpec {
  val upperCasePrefix = VaultTagKey.prefix.toUpperCase

  "Tag.Key.fromString" should "return Tag.Key.VaultRO" in {
    val testString = "VAULT.RO"
    assert(VaultTagKey.fromString(s"$upperCasePrefix$testString") == VaultTagKey.VaultRO)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultRW" in {
    val testString = "VAULT.RW"
    assert(VaultTagKey.fromString(s"$upperCasePrefix$testString") == VaultTagKey.VaultRW)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultHost" in {
    val testString = "VAULT.HOSTS"
    assert(VaultTagKey.fromString(s"$upperCasePrefix$testString") == VaultTagKey.VaultHost)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultPrefix" in {
    val testString = "VAULT.PREFIX"
    assert(VaultTagKey.fromString(s"$upperCasePrefix$testString") == VaultTagKey.VaultPrefix)
  }

  "Tag.Key.toString" should "return vault.ro" in {
    val testString = "vault.ro"
    assert(VaultTagKey.toString(VaultTagKey.VaultRO) == s"${VaultTagKey.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.rw" in {
    val testString = "vault.rw"
    assert(VaultTagKey.toString(VaultTagKey.VaultRW) == s"${VaultTagKey.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.host" in {
    val testString = "vault.host"
    assert(VaultTagKey.toString(VaultTagKey.VaultHost) == s"${VaultTagKey.prefix}$testString")
    val testString = "vault.hosts"
    assert(Tag.Key.toString(Tag.Key.VaultHost) == s"${Tag.Key.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.prefix" in {
    val testString = "vault.prefix"
    assert(VaultTagKey.toString(VaultTagKey.VaultPrefix) == s"${VaultTagKey.prefix}$testString")
  }
}
