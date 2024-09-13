package org.wisp.bus

trait Event{

  def stackTrace:Option[Throwable] = None

}