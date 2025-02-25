package org.wisp.stream.iterator

import org.wisp.exceptions.ExceptionHandler
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}
import org.wisp.ActorLink

import java.util
import scala.collection.mutable

class ZipStream(eh:ExceptionHandler, prev:Iterable[ActorLink]) extends StreamActorLink, ActorLink{
  def this(handler:ExceptionHandler, l:ActorLink*) = this(handler, l)

  protected val nodes: util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected class State(val link:ActorLink){
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
        link.ask(HasNext).whenComplete(eh >> ZipStream.this)
      }
    }

    def send(ref: ActorLink):Unit = {
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

      val n = nodes.poll()
      if(n == null){
        value = Some(v)
      }else{
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
    i.find(_.hasValue)
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      select(state.values) match {
        case Some(n) =>
          n.send(sender)

        case None =>
          if(state.values.forall(_.isFinished)){
            sender << End
          }else{
            nodes.add(sender)
            for(x <- state.values) x.requestNext()
          }
      }

    case Next(v) =>
      state(sender).next(v)

    case End =>
      state(sender).end()

      if(state.values.forall(_.isFinished)){
        var a = nodes.poll()
        while (a != null) {
          a << End
          a = nodes.poll()
        }
      }

  }

}
