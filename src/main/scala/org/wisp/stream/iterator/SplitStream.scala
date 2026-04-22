package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.utils.lock.*

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext

/**
 * Duplicate `original` stream into links created with `link.copy`
 * Data from `original` is pulled after every link created with `link.copy` is pulled.
 */
class SplitStream[T](original:ActorLink[Operation[T]])(link: SplitStream[T]#Split => Unit)(using ExecutionContext) extends StreamActorLink[T] {

  protected override val lock:ReentrantLock = new ReentrantLock()

  trait Split {
    def copy: ActorLink[Operation[T]]
  }

  protected class SplitBuilder extends Split {
    var links:List[SplitActorLink] = Nil

    override def copy: SplitActorLink = {
      val link = SplitActorLink()
      links = link :: links
      link
    }

  }

  protected def createNodes(): util.Queue[ActorLink[Operation[T]]] = {
    util.LinkedList[ActorLink[Operation[T]]]()
  }

  protected var requested = true

  protected var ended = false

  protected val nextTo: List[SplitActorLink] = {
    val s = SplitBuilder()
    link.apply(s)
    s.links
  }

  lock.withLock {
    requested = false
    pullNext()
  }

  override def apply(from: ActorLink[Operation[T]]): PartialFunction[Operation[T], Unit] = {
    case Next(v) =>
      if(!requested) throw new IllegalStateException("not requested")
      requested = false

      for (n <- nextTo) n.next(v)
      pullNext()
    case End =>
      if(!requested) throw new IllegalStateException("not requested")
      requested = false

      ended = true
      for (n <- nextTo) n.end()
  }

  protected def pullNext():Unit = {
    if (!requested && nextTo.forall(i => !i.nodes.isEmpty)) {
      requested = true
      original.call(HasNext).onComplete(SplitStream.this.apply)
    }
  }

  protected class SplitActorLink extends StreamActorLink[T]{

    override protected def lock: ReentrantLock = SplitStream.this.lock

    val nodes: util.Queue[ActorLink[Operation[T]]] = createNodes()

    def next(v:T): Unit = {
      val n = nodes.poll()
      if(n == null) throw new IllegalStateException("nodes are empty")
      n << Next(v)
    }

    def end(): Unit = {
      var n = nodes.poll()
      if(n == null) throw new IllegalStateException("nodes are empty")
      while(n != null){
        n << End
        n = nodes.poll()
      }
    }


    override def apply(from: ActorLink[Operation[T]]): PartialFunction[Operation[T], Unit] = {
      case HasNext =>
        if (ended) {
          from << End
        } else {
          nodes.add(from)
          pullNext()
        }
    }

  }

}
