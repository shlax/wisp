package org.qwActor.remote.codec

import java.nio.ByteBuffer

trait Decoder {

  def read(buffer:ByteBuffer):Boolean

}
