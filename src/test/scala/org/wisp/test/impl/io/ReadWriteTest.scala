package org.wisp.test.impl.io

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.closeable.*
import org.wisp.io.codec.*

class ReadWriteTest {

  @Test
  def baseTest(): Unit = {
    val id1 = IdName(1, "test")

    val out = new ByteArrayOutputStream()
    new ObjectOutputStream(out) | { os =>
      id1.write(os)
    }

    val in = new ByteArrayInputStream(out.toByteArray)
    val id2 = new ObjectInputStream(in)|{ is =>
      read[IdName](is)
    }

    Assertions.assertEquals(id1, id2)

  }

}
