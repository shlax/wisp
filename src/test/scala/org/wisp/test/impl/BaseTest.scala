package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
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

}
