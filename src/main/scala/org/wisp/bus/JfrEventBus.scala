package org.wisp.bus

import java.util.logging.{Logger, Level}

class JfrEventBus extends EventBus {
  private val logger : Logger = Logger.getLogger("org.wisp")

  override def publish(event: Event): Unit = {
    val e = new WispEvent
    if (e.isEnabled && e.shouldCommit) {
      e.message = event.toString
      e.commit()
    }

    if(logger.isLoggable(Level.WARNING)) {
      event.stackTrace match {
        case Some(e) =>
          logger.log(Level.WARNING, event.toString, e)
        case None =>
          logger.log(Level.WARNING, event.toString)
      }
    }

  }

}
