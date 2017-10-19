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
package com.bwsw.cloudstack.vault.server.common

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class WeightedQueueTestSuite extends FlatSpec with Matchers {
  val firstElement = "1"
  val secondElement = "2"
  val thirdElement = "3"

  "moveElementToEnd" should "move the first element to the end of the queue" in {
    val expectedResult = List(secondElement, thirdElement, firstElement)
    val queue = new WeightedQueue[String](List(firstElement, secondElement, thirdElement))

    queue.moveElementToEnd(firstElement)

    assert(expectedResult == queue.getElements)
  }

  "moveElementToEnd" should "move the second element to the end of the queue" in {
    val expectedResult = List(firstElement, thirdElement, secondElement)
    val queue = new WeightedQueue[String](List(firstElement, secondElement, thirdElement))

    queue.moveElementToEnd(secondElement)

    assert(expectedResult == queue.getElements)
  }

  "moveElementToEnd" should "not change a queue if an element is last" in {
    val expectedResult = List(firstElement, secondElement, thirdElement)
    val queue = new WeightedQueue[String](List(firstElement, secondElement, thirdElement))

    queue.moveElementToEnd(thirdElement)

    assert(expectedResult == queue.getElements)
  }

  "moveElementToEnd" should "throw IllegalArgumentException if Initialize elements list is not contain element for move" in {
    val expectedResult = List(firstElement, secondElement, thirdElement)
    val queue = new WeightedQueue[String](List(firstElement, secondElement, thirdElement))

    assertThrows[IllegalArgumentException] {
      queue.moveElementToEnd("test")
    }
  }

  "getElement" should "return first element" in {
    val expectedResult = firstElement
    val queue = new WeightedQueue[String](List(firstElement, secondElement, thirdElement)) {
      override val r = new Random {
        override def nextInt(n: Int): Int = 0
      }
    }

    assert(firstElement == queue.getElement)
  }

  "getElement" should "return second element" in {
    val expectedResult = secondElement
    val queue = new WeightedQueue[String](List(firstElement, secondElement, thirdElement)) {
      override val r = new Random {
        override def nextInt(n: Int): Int = 3
      }
    }

    assert(expectedResult == queue.getElement)
  }
}
