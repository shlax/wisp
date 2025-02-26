package org.wisp.stream.iterator

import org.wisp.lock.*
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.message.IteratorMessage
import org.wisp.{ActorLink, Message}

import java.io.Flushable
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import scala.util.control.NonFatal

abstract class StreamActorLink extends Consumer[Message], StreamException{

  protected val lock = new ReentrantLock()

  /** method is running with lock */
  def accept(from:ActorLink): PartialFunction[IteratorMessage, Unit]

  override def accept(t: Message): Unit = {
    val f = accept(t.sender)
    lock.withLock{ f.apply(t.value.asInstanceOf[IteratorMessage]) }
  }

  protected def autoClose(c: AutoCloseable, tr: Option[Throwable]): Unit = {
    try {
      c.close()
    } catch {
      case NonFatal(ex) =>
        if (tr.isDefined) {
          val e = tr.get
          e.addSuppressed(ex)
          throw e
        } else {
          throw ex
        }
    }

    for(e <- tr){
      throw e
    }

  }

  protected def autoClose(c:Sink[?])(block: => Unit):Unit = {
    autoClose(c, Some(c), block)
  }

  protected def autoClose(c: Source[?])(block: => Unit): Unit = {
    autoClose(c, None, block)
  }

  protected def autoClose(c:AutoCloseable, flush:Option[Flushable], block: => Unit):Unit = {
    var e: Throwable = null
    try{
      block
      for(f <- flush){
        f.flush()
      }
    }catch{
      case NonFatal(ex) =>
        e = ex
    }
    try{
      c.close()
    }catch{
      case NonFatal(ex) =>
        if(e != null) e.addSuppressed(ex)
        else e = ex
    }
    if(e != null){
      throw e
    }
  }

}
