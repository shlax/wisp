package org.wisp.exceptions

trait ExceptionHandler {

  def handle(exception: Throwable): Unit = {
    exception.printStackTrace()
  }

}
