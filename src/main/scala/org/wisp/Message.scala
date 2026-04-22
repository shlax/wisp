package org.wisp

import org.wisp.jfr.{MessageCreated, MessageProcessed}
import java.util.UUID

/**
 * Represents a message passed between actors
 *
 * @param sender the actor link of the message sender
 * @param value  the payload of the message
 */
case class Message[+T](sender:ActorLink[?], value:T) {

  val jfrId:Option[UUID] = {
    val event = MessageCreated()
    if(event.shouldCommit){
      val id = UUID.randomUUID()
      event.uuid = id.toString
      if(value != null) {
        event.value = value.toString
      }
      event.commit()
      Some(id)
    }else None
  }

  /** capture JFR data related to processing this message */
  def process[R](consumerClass: => Class[?])(fn: => R) : R = {
    val event = MessageProcessed()
    event.begin()
    try{
      fn
    }finally {
      event.end()
      if (event.shouldCommit) {
        event.consumer = consumerClass
        for (id <- jfrId) {
          event.uuid = id.toString
        }
        if (value != null) {
          event.value = value.toString
        }
        event.commit()
      }
    }
  }

}
