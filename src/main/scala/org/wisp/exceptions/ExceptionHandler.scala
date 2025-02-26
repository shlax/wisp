package org.wisp.exceptions

trait ExceptionHandler{

  def onException(exception: Throwable): Unit = {
    exception.printStackTrace()
  }

}
