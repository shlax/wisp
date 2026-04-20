package org.wisp.stream.iterator

import org.wisp.stream.iterator.message.{End, HasNext, Next, Operation}
import org.wisp.{ActorLink, Message}
import org.wisp.utils.lock.*
import java.util
import scala.concurrent.ExecutionContext

/**
 * Combine multiple `streams` into one
 * @param streams streams to combine
 */
class ZipStream(streams:Iterable[ActorLink])(using ExecutionContext) extends StreamActorLink, ActorLink, SingleNodeFlow{
  def this(l:ActorLink*)(using ExecutionContext) = this(l)

  protected override val nodes: util.Queue[ActorLink] = createNodes()

  protected class State(val link:ActorLink) extends StreamConsumer {
    protected var value:Option[Any] = None

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

    def send(ref: ActorLink):Unit = {
      val v = value.get
      value = None
      ref << Next(v)
      requestNext()
    }

    override def apply(m: Message): Unit = lock.withLock {
      m.process(ZipStream.this.getClass) {
        m.value match {
          case Next(v) =>
            next(v)
          case End =>
            end()
        }
      }
    }

    def next(v:Any) :Unit = {
      if(ended) throw new IllegalStateException("ended: "+v)
      if(!requested) throw new IllegalStateException("not requested: "+v)
      if(value.isDefined) throw new IllegalStateException("dropped: "+v)

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

  protected def createState(link:ActorLink): State = {
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

  override def apply(sender: ActorLink): PartialFunction[Operation, Unit] = {
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
