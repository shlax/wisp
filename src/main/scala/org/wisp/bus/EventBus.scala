package org.wisp.bus

trait EventBus {

  def publish(event:Any):Unit

}
