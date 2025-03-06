package org.wisp.stream.iterator

import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}
import org.wisp.ActorLink

import java.util
import scala.collection.mutable
import scala.concurrent.ExecutionContext

class ZipStream(prev:Iterable[ActorLink])(using executor: ExecutionContext) extends StreamActorLink, ActorLink{
  def this(l:ActorLink*)(using executor: ExecutionContext) = this(l)

  protected val nodes: util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected var exception: Option[Throwable] = None

  protected class State(val link:ActorLink){
    protected var value:Option[Any] = None

    protected var requested = false
    protected var ended = false

    def isFinished:Boolean = {
      ended && !requested && value.isEmpty
    }

    def hasValue:Boolean = {
      if(exception.isDefined) throw new IllegalStateException("exception")

      value.isDefined
    }

    def requestNext():Unit = {
      if(exception.isDefined) throw new IllegalStateException("exception")

      if (!ended && !requested && value.isEmpty) {
        requested = true
        link.ask(HasNext).onComplete(accept)
      }
    }

    def send(ref: ActorLink):Unit = {
      if(exception.isDefined) throw new IllegalStateException("exception")

      val v = value.get
      value = None
      ref << Next(v)
      requestNext()
    }

    def next(v:Any) :Unit = {
      if(ended) throw new IllegalStateException("ended: "+v)
      if(!requested) throw new IllegalStateException("not requested: "+v)
      if(value.isDefined) throw new IllegalStateException("dropped: "+v)

      requested = false

      if(exception.isEmpty) {
        val n = nodes.poll()
        if (n == null) {
          value = Some(v)
        } else {
          n << Next(v)
          requestNext()
        }
      }

    }

    def end(ex: Option[Throwable]):Unit = {
      if(ended) throw new IllegalStateException("ended")
      if(!requested) throw new IllegalStateException("not requested")
      if(value.isDefined) throw new IllegalStateException("dropped: "+value.get)

      if(ex.isDefined){
        exception = ex

        var n = nodes.poll()
        while (n != null) {
          n << End(exception)
          n = nodes.poll()
        }
      }else {
        requested = false
        ended = true

        if(state.values.forall(_.isFinished)){
          var n = nodes.poll()
          while (n != null) {
            n << End(exception)
            n = nodes.poll()
          }
        }

      }

    }

  }

  protected def createState(link:ActorLink): State = {
    State(link)
  }

  protected val state:Map[ActorLink, State] = {
    val m = mutable.Map[ActorLink, State]()
    for(p <- prev) m(p) = createState(p)
    m.toMap
  }

  protected def select(i:Iterable[State]) : Option[State] = {
    if(exception.isDefined) throw new IllegalStateException("exception")

    i.find(_.hasValue)
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if(exception.isDefined){
        sender << End(exception)
      }else {
        select(state.values) match {
          case Some(n) =>
            n.send(sender)

          case None =>
            if (state.values.forall(_.isFinished)) {
              sender << End()
            } else {
              nodes.add(sender)
              for (x <- state.values) x.requestNext()
            }
        }
      }

    case Next(v) =>
      state(sender).next(v)

    case End(ex) =>
      state(sender).end(ex)

  }

}
