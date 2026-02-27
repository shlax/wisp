package org.wisp.test.impl.io

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.closeable.*
import org.wisp.io.extensions.*

class ReadWriteTest {

  @Test
  def caseTest(): Unit = {
    val id1 = IdName(1, "test")

    val out = new ByteArrayOutputStream()
    new DataOutputStream(out) | { os =>
      id1.ioWrite(os)
    }

    val in = new ByteArrayInputStream(out.toByteArray)
    val id2 = new DataInputStream(in)|{ is =>
      ioRead[IdName](is)
    }

    Assertions.assertEquals(id1, id2)

  }

  @Test
  def enumTest(): Unit = {
    val id1 = IdEnum.WRITE("7")

    val out = new ByteArrayOutputStream()
    new DataOutputStream(out) | { os =>
      id1.ioWrite(os)
    }

    val in = new ByteArrayInputStream(out.toByteArray)
    val id2 = new DataInputStream(in) | { is =>
      ioRead[IdEnum](is)
    }

    Assertions.assertEquals(id1, id2)

  }

  @Test
  def combineTest(): Unit = {
    val id1 = IdEnum.EXEC(IdName(1, "test"))

    val out = new ByteArrayOutputStream()
    new DataOutputStream(out) | { os =>
      id1.ioWrite(os)
    }

    val in = new ByteArrayInputStream(out.toByteArray)
    val id2 = new DataInputStream(in) | { is =>
      ioRead[IdEnum](is)
    }

    Assertions.assertEquals(id1, id2)

  }

  @Test
  def simpleEnumTest(): Unit = {
    val id1 = IdMode.Y

    val out = new ByteArrayOutputStream()
    new DataOutputStream(out) | { os =>
      id1.ioWrite(os)
    }

    val in = new ByteArrayInputStream(out.toByteArray)
    val id2 = new DataInputStream(in) | { is =>
      ioRead[IdMode](is)
    }

    Assertions.assertEquals(id1, id2)

  }

  @Test
  def combineSimpleTest(): Unit = {
    val id1 = IdEnum.READ(IdMode.Z)

    val out = new ByteArrayOutputStream()
    new DataOutputStream(out) | { os =>
      id1.ioWrite(os)
    }

    val in = new ByteArrayInputStream(out.toByteArray)
    val id2 = new DataInputStream(in) | { is =>
      ioRead[IdEnum](is)
    }

    Assertions.assertEquals(id1, id2)

  }

}
