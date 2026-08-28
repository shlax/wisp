package org.wisp.stream.iterator

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContextExecutor

/**
 * Combine multiple `streams` into one
 * @param streams streams to combine
 */
class ZipStream[T](streams:Iterable[StreamFlow[T]])(using ExecutionContextExecutor) extends SingleNodeFlow[T], ExecutionFlow[T]{
  def this(l:StreamFlow[T]*)(using ExecutionContextExecutor) = this(l)

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected override val nodes: util.Queue[Option[T] => Unit] = createNodes()

  protected class State(val link:StreamFlow[T]) extends StreamHandler[T] {

    protected override val lock:ReentrantLock = ZipStream.this.lock

    protected var value:Option[T] = None

    protected var requested = false
    protected var ended = false

    def isFinished:Boolean = {
      ended && !requested && value.isEmpty
    }

    def hasValue:Boolean = {
      value.isDefined
    }

    def requestNext():Unit = {
      if (!ended && !requested && value.isEmpty) {
        requested = true
        link.next(State.this)
      }
    }

    def send(ref: Option[T] => Unit):Unit = {
      val v = value.get
      value = None
      ref.apply(Some(v))
      requestNext()
    }

    override def applyWithLock(rv: Option[T]): Unit = rv match {
      case Some(v) =>
        next(v)
      case None =>
        end()
    }

    def next(v:T) :Unit = {
      if(ended) throw new IllegalStateException("ended: "+v)
      if(!requested) throw new IllegalStateException("not requested: "+v)
      if(value.isDefined) throw new IllegalStateException("dropped: "+value.get)

      requested = false

      val n = nodes.poll()
      if (n == null) {
        value = Some(v)
      } else {
        n.apply(Some(v))
        requestNext()
      }

    }

    def end():Unit = {
      if(ended) throw new IllegalStateException("ended")
      if(!requested) throw new IllegalStateException("not requested")
      if(value.isDefined) throw new IllegalStateException("dropped: "+value.get)

      requested = false
      ended = true

      if(state.forall(_.isFinished)){
        sendEnd()
      }

    }

  }

  protected def createState(link:StreamFlow[T]): State = {
    State(link)
  }

  protected val state:List[State] = {
    val b = List.newBuilder[State]
    for(p <- streams) b += createState(p)
    b.result()
  }

  /**
   * find the first upstream link with value
   */
  protected def select: Option[State] = {
    state.find(_.hasValue)
  }

  override def nextWithLock(sender: Option[T] => Unit): Unit = {
    select match {
      case Some(n) =>
        n.send(sender)

      case None =>
        if (state.forall(_.isFinished)) {
          sender.apply(None)
        } else {
          nodes.add(sender)
          for (x <- state) x.requestNext()
        }
    }
  }

}
