package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.exceptions.UndeliveredException
import org.wisp.{Link, Message}

class BaseTest {

  @Test
  def message():Unit = {
    var res = 0

    val m:Message[Int, Int] = Message(2, new Link[Int, Int] {
      override def apply(t: Message[Int, Int]): Unit = {
        res += t.value
      }
    })

    m match {
      case Message(a, b) =>
        b << a + 1
    }

    Assertions.assertEquals(3, res)

  }

  @Test
  def undeliveredException(): Unit = {
    val l:Link[Int, Int] = new Link[Int, Int] {
      override def apply(t: Message[Int, Int]): Unit = {
        t.sender << t.value + 1
      }
    }

    var m:Option[Message[?, ?]] = None

    try {
      l << 2
    }catch {
      case UndeliveredException(msg) =>
        m = Some(msg)
    }

    Assertions.assertEquals(m.get.value, 3)

  }

}
