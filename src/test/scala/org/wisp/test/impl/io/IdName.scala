package org.wisp.test.impl.io

import org.wisp.io.ReadWrite
import org.wisp.io.codec.given

case class IdName (id:Int, name:String) derives ReadWrite
