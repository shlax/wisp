package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.utils.lock.*
import org.wisp.stream.iterator.message.{End, HasNext, Next, Operation}

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext

/**
 * Duplicate `original` stream into links created with `link.copy`
 * Data from `original` is pulled after every link created with `link.copy` is pulled.
 */
class SplitStream(original:ActorLink)(link: SplitStream#Split => Unit)(using ExecutionContext) extends StreamActorLink {

  protected override val lock:ReentrantLock = new ReentrantLock()
  
  trait Split {
    def copy: ActorLink
  }

  protected class SplitBuilder extends Split {
    var links:List[SplitActorLink] = Nil

    override def copy: SplitActorLink = {
      val link = SplitActorLink()
      links = link :: links
      link
    }

  }

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
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

  override def apply(from: ActorLink): PartialFunction[Operation, Unit] = {
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

  protected class SplitActorLink extends ActorLink, StreamActorLink{

    override protected def lock: ReentrantLock = SplitStream.this.lock

    val nodes: util.Queue[ActorLink] = createNodes()

    def next(v:Any): Unit = {
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

    
    override def apply(from: ActorLink): PartialFunction[Operation, Unit] = {
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
