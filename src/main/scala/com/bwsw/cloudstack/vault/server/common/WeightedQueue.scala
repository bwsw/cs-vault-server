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

import com.google.common.collect.{Range,RangeMap,TreeRangeMap}

import scala.util.Random

/**
  * Class is responsible for obtaining a list item with a probability calculated by the formula:
  * (lengthOfList - indexOfElement)/sumOfAllNumbersOfList
  * where:
  * "lengthOfList" - length of initialization list with elements
  * "indexOfElement" - index of element in the list
  * "sumOfAllNumbersOfList" - sum of numbers from 1 to "lengthOfList"
  */
class WeightedQueue[T](private val elementList: List[T]) {
  protected val r = new Random

  private val lengthOfArray = elementList.length
  private val sumOfNumbers = getSumOfNumbers(0, lengthOfArray)
  private val gradeScaleElements: RangeMap[Integer, Int] = TreeRangeMap.create()

  private var elements = elementList

  fillRangeMap(0, lengthOfArray, 0)

  def getElement: T = elements(gradeScaleElements.get(r.nextInt(sumOfNumbers)))

  def getElements: List[T] = elements

  def moveElementToEnd(element: T): Unit = synchronized {
    val currentIndex = elements.indexOf(element)
    if(currentIndex != -1) {
      if (currentIndex != lengthOfArray - 1) {
        elements = elements.take(currentIndex) ++ elements.takeRight(lengthOfArray - currentIndex - 1) :+ element
      }
    } else {
      throw new IllegalArgumentException(s"Element: $element is not included in the initialization list: $elementList")
    }
  }

  private def fillRangeMap(startOfRange: Int, range: Int, index: Int): Unit = {
    if (range != 0) {
      gradeScaleElements.put(Range.closed(startOfRange, startOfRange + range - 1), index)
      fillRangeMap(startOfRange + range, range - 1, index + 1)
    }
  }

  private def getSumOfNumbers(acc: Int, lenght: Int): Int = {
    if(lenght == 0) {
      acc
    } else {
      getSumOfNumbers(acc + lenght, lenght - 1)
    }
  }
}
