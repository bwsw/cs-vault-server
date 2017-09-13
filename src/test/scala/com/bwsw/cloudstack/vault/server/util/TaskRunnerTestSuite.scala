package com.bwsw.cloudstack.vault.server.util

import com.bwsw.cloudstack.vault.server.util.exception.CriticalException
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 06.09.17.
  */
class TaskRunnerTestSuite extends FlatSpec {
  "tryRunUntilSuccess" should "if non-CriticalException was be thrown it must rerun self " +
    "and then it must return string result" in {
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

  "tryRunUntilSuccess" should "not rerun self if CriticalException was be thrown" in {

    def testTask:() => String = () => {
      throw new CriticalException(new Exception)
    }

    assertThrows[CriticalException]{
      TaskRunner.tryRunUntilSuccess[String](testTask, 0)
    }
  }
}
