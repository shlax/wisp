package org.wisp.test.impl.io

import org.wisp.io.ReadWrite
import org.wisp.io.extensions.given

enum IdEnum derives ReadWrite{

  case READ(id:IdMode)
  case WRITE(id:String)
  case EXEC(id:IdName)

}
