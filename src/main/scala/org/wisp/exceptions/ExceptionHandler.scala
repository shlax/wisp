package org.wisp.exceptions

import java.util.function.{BiConsumer, Consumer}
import scala.annotation.targetName
import scala.util.control.NonFatal

trait ExceptionHandler{

  def onException(exception: Throwable): Unit = {
    exception.printStackTrace()
  }

}
