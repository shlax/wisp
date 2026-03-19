package org.wisp

import java.nio.ByteBuffer

package object utils {

  /**
   * @param data unsigned int (0 - 4,294,967,295)
   * @return packed byte array [4]
   */
  def unsignedIntToBytes(data:Long): Array[Byte] = {
    val res = new Array[Byte](4)
    ByteBuffer.wrap(res).putInt(data.toInt)
    res
  }

  def bytesToUnsignedInt(data:Array[Byte]):Long = {
    ByteBuffer.wrap(data, 0, 4).getInt & 0xffffffffL
  }

}
