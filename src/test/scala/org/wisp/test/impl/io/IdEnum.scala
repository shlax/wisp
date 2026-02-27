package org.wisp.test.impl.io

import org.wisp.io.ReadWrite
import org.wisp.io.codec.given

enum IdEnum derives ReadWrite{

  case READ(id:String)
  case WRITE(id:String)
  case EXEC(id:String)

}
