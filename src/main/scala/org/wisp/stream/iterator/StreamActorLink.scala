package org.wisp.stream.iterator

import org.wisp.lock.*
import org.wisp.stream.iterator.message.IteratorMessage
import org.wisp.{ActorLink, Message}

import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import scala.util.control.NonFatal

abstract class StreamActorLink extends Consumer[Message]{

  protected val lock = new ReentrantLock()

  def accept(from:ActorLink): PartialFunction[IteratorMessage, Unit]

  override def accept(t: Message): Unit = {
    val f = accept(t.sender)
    lock.withLock{ f.apply(t.value.asInstanceOf[IteratorMessage]) }
  }

  protected def autoClose(c: Consumer[?], tr: Option[Throwable]): Unit = {
    c match {
      case ac: AutoCloseable =>
        try {
          ac.close()
        } catch {
          case NonFatal(ex) =>
            for(e <- tr) ex.addSuppressed(e)
            throw ex
        }
      case _ =>
        for(e <- tr) throw e
    }
  }

  protected def autoClose(c:Consumer[?])(block: => Unit):Unit = {
    var e: Throwable = null
    try{
      block
    }catch{
      case NonFatal(ex) =>
        e = ex
    }finally {
      c match{
        case ac:AutoCloseable =>
          try{
            ac.close()
          }catch{
            case NonFatal(ex) =>
              if(e != null) ex.addSuppressed(e)
              e = ex
          }
        case _ =>
      }
    }
    if(e != null){
      throw e
    }
  }

}
