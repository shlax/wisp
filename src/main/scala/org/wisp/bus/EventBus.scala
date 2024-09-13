package org.wisp.bus

trait EventBus {

  def publish(event:Event):Unit

}
