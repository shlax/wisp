package org.wisp.test.impl.io

import org.wisp.serializer.ReadWrite
import org.wisp.serializer.given

enum IdEnum derives ReadWrite{

  case READ(id:IdMode)
  case WRITE(id:String)
  case EXEC(id:IdName)

}
