package org.wisp

import org.wisp.jfr.{MessageCreated, MessageProcessed}
import java.util.concurrent.ThreadLocalRandom
import java.util.UUID

object Message {

  def randomUUID(): UUID = {
    val r = ThreadLocalRandom.current()
    val leastSigBits = r.nextLong()
    val mostSigBits = r.nextLong()
    new UUID(mostSigBits, leastSigBits)
  }

}

/**
 * Represents value and callback passed between [[Link]]s
 *
 * @param value the payload of the message
 * @param sender sender of the message
 */
class Message[+T, -R](val value:T, val sender:Link[R, T]) {

  val jfrId:Option[UUID] = {
    val event = MessageCreated()
    if(event.shouldCommit){
      val id = Message.randomUUID()
      event.uuid = id.toString
      if(value != null) {
        event.value = value.toString
      }
      event.commit()
      Some(id)
    }else None
  }

  /**
   * capture JFR data related to processing this message
   */
  def process[V](consumerClass: => Class[?])(fn: => V) : V = {
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

  override def toString: String = {
    if(jfrId.isDefined) {
      s"Message[${jfrId.get}]($value)"
    }else {
      s"Message($value)"
    }
  }

}
