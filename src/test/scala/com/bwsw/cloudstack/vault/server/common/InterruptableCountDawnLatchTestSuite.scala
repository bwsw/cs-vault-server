package com.bwsw.cloudstack.vault.server.common

import java.util.concurrent.CountDownLatch

import com.bwsw.cloudstack.vault.server.util.exception.AbortedException
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InterruptableCountDawnLatchTestSuite extends FlatSpec with Matchers {

  "await" should "successful" in {
    val interruptableCountDawnLatch = new InterruptableCountDawnLatch(new CountDownLatch(1))

    Future {
      Thread.sleep(1000)
      interruptableCountDawnLatch.succeed()
    }

    assert(interruptableCountDawnLatch.await().isInstanceOf[Unit])
  }

  "await" should "thrown AbortedException" in {
    val interruptableCountDawnLatch = new InterruptableCountDawnLatch(new CountDownLatch(1))

    Future {
      Thread.sleep(1000)
      interruptableCountDawnLatch.abort()
    }

    assertThrows[AbortedException](interruptableCountDawnLatch.await().isInstanceOf[Unit])
  }
}
