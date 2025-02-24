package org.wisp.exceptions

import org.wisp.Message

import java.util.function.{BiConsumer, Consumer}
import scala.annotation.targetName
import scala.util.control.NonFatal

trait ExceptionHandler{

  @targetName("forwardException")
  def >>[T](c:Consumer[T]):BiConsumer[T, Throwable] = { (v, e) =>
    if(e != null) onException(e)
    if(v != null){
      try {
        c.accept(v)
      }catch{
        case NonFatal(e) =>
          onException(e)
      }
    }
  }

  def onException(exception: Throwable): Unit = {
    exception.printStackTrace()
  }

}
