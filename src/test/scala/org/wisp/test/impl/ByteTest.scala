package org.wisp.test.impl

import org.wisp.utils.*
import org.junit.jupiter.api.{Assertions, Test}

class ByteTest {

  @Test
  def testMin():Unit = {
    val a = 0L
    val b = unsignedIntToBytes(a)
    val c = bytesToUnsignedInt(b)
    Assertions.assertEquals(a, c)
  }

  @Test
  def testMax(): Unit = {
    val a = 4_294_967_295L
    val b = unsignedIntToBytes(a)
    val c = bytesToUnsignedInt(b)
    Assertions.assertEquals(a, c)
  }

  @Test
  def test(): Unit = {
    val a = 123456L
    val b = unsignedIntToBytes(a)
    val c = bytesToUnsignedInt(b)
    Assertions.assertEquals(a, c)
  }

}
