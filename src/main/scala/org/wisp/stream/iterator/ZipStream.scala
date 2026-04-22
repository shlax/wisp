package org.wisp.stream.iterator

import org.wisp.Link
import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext

/**
 * Combine multiple `streams` into one
 * @param streams streams to combine
 */
class ZipStream[T](streams:Iterable[Link[Operation[T], Operation[T]]])(using ExecutionContext) extends StreamLink[T], SingleNodeFlow[T]{
  def this(l:Link[Operation[T], Operation[T]]*)(using ExecutionContext) = this(l)

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected override val nodes: util.Queue[Link[Operation[T], Operation[T]]] = createNodes()

  protected class State(val link:Link[Operation[T], Operation[T]]) extends StreamLink[T] {

    protected override def lock:ReentrantLock = ZipStream.this.lock

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
        link.call(HasNext).onComplete(State.this.apply)
      }
    }

    def send(ref: Link[Operation[T], Operation[T]]):Unit = {
      val v = value.get
      value = None
      ref << Next(v)
      requestNext()
    }

    override def apply(from: Link[Operation[T], Operation[T]]): PartialFunction[Operation[T], Unit] = {
      case Next(v) =>
        next(v)
      case End =>
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
        n << Next(v)
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

  protected def createState(link:Link[Operation[T], Operation[T]]): State = {
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

  override def apply(sender: Link[Operation[T], Operation[T]]): PartialFunction[Operation[T], Unit] = {
    case HasNext =>
      select match {
        case Some(n) =>
          n.send(sender)

        case None =>
          if (state.forall(_.isFinished)) {
            sender << End
          } else {
            nodes.add(sender)
            for (x <- state) x.requestNext()
          }
      }
  }

}
