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

class CircleQueue[T](private val elementList: List[T]) {
  protected var elements = elementList

  def getElement: T = elements.head

  def getElements: List[T] = elements

  def moveElementToEnd(element: T): Unit = synchronized {
    if (elements.contains(element)) {
      elements match {
        case x :: xs if x == element =>
          elements = xs ::: x :: Nil
        case _ =>
       }
    } else {
      throw new IllegalArgumentException(s"Element: $element is not included in the initialization list: $elementList")
    }
  }
}
