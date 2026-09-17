package org.wisp.test.impl

import org.junit.jupiter.api.Test
import org.wisp.timer.Timer
import org.wisp.utils.closeable.*

import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.*

class TimerTest {

  @Test
  def timer(): Unit = {
    val i = new AtomicInteger(0)
    val s = new AtomicInteger(0)
    val cd = new CountDownLatch(1)

    Timer()|{ t =>

      t.scheduleAtFixedRate[Int](50.millis, 50.millis, i.incrementAndGet()){ o =>
        o.map( _ * 2 ).to{ v =>
          println(v)
          if( s.addAndGet(v) > 3) {
            cd.countDown()
          }
        }
      }

      cd.await(10, TimeUnit.SECONDS)

    }
  }

}
