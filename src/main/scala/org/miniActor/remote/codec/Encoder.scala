package org.miniActor.remote.codec

import java.nio.ByteBuffer

trait Encoder {

  def write(buffer:ByteBuffer):Option[Boolean]

}
