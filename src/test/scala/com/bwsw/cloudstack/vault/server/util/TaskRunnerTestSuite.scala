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
package com.bwsw.cloudstack.vault.server.util

import com.bwsw.cloudstack.vault.server.util.exception.CriticalException
import org.scalatest.FlatSpec

class TaskRunnerTestSuite extends FlatSpec {
  "tryRunUntilSuccess" should "run again task processing if non-CriticalException is thrown " +
    "and then method returns an expected result after recovery" in {
    var countOfTaskRunning = 4
    val expectedResult = "test"

    def testTask:() => String = () => {
      if (countOfTaskRunning != 0) {
        countOfTaskRunning = countOfTaskRunning - 1
        throw new Exception
      } else {
        expectedResult
      }
    }

    val actualResult = TaskRunner.tryRunUntilSuccess[String](testTask, 0)

    assert(actualResult == expectedResult)
  }

  "tryRunUntilSuccess" should "not run again task processing if CriticalException is thrown" in {

    def testTask:() => String = () => {
      throw new CriticalException("test exception")
    }

    assertThrows[CriticalException]{
      TaskRunner.tryRunUntilSuccess[String](testTask, 0)
    }
  }
}
