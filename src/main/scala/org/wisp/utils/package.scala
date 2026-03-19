package org.wisp

import java.nio.ByteBuffer

package object utils {

  /**
   * @param data unsigned int
   * @return packed byte array [4]
   */
  def unsignedIntToBytes(data:Long): Array[Byte] = {
    val res = new Array[Byte](4)
    ByteBuffer.wrap(res).putInt(data.toInt)
    res
  }

  /**
   * @param data packed byte array [4]
   * @return unsigned int
   */
  def bytesToUnsignedInt(data:Array[Byte]):Long = {
    Integer.toUnsignedLong(ByteBuffer.wrap(data, 0, 4).getInt)
  }

}
