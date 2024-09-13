package org.wisp.bus

class JfrEventBus extends EventBus {

  override def publish(event: Event): Unit = {
    val e = new WispEvent
    if (e.isEnabled && e.shouldCommit) {
      e.message = event.toString
      e.commit()
    }
  }

}
