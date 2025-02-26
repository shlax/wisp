package org.wisp.stream.iterator

import org.wisp.lock.*
import org.wisp.stream.Source
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

  protected def autoFlush(c: Any):Unit = {
    c match {
      case f: Flushable =>
        f.flush()
      case _ =>
    }
  }

  protected def autoClose(c: Any, tr: Option[Throwable]): Unit = {
    c match {
      case ac: AutoCloseable =>
        try {
          ac.close()
        } catch {
          case NonFatal(ex) =>
            if(tr.isDefined){
              val e = tr.get
              e.addSuppressed(ex)
              throw e
            }else {
              throw ex
            }
        }
      case _ =>
        for(e <- tr) throw e
    }
  }

  protected def autoClose(c:Consumer[?])(block: => Unit):Unit = {
    autoClose(c, true, block)
  }

  protected def autoClose(c: Source[?])(block: => Unit): Unit = {
    autoClose(c, false, block)
  }

  protected def autoClose(c:Any, flush:Boolean, block: => Unit):Unit = {
    var e: Throwable = null

    try{
      block
      if(flush) {
        autoFlush(c)
      }
    }catch{
      case NonFatal(ex) =>
        e = ex
    }

    c match{
      case ac:AutoCloseable =>
        try{
          ac.close()
        }catch{
          case NonFatal(ex) =>
            if(e != null) e.addSuppressed(ex)
            else e = ex
        }
      case _ =>
    }

    if(e != null){
      throw e
    }

  }

}
