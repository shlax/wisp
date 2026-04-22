package org.wisp.stream.iterator

import org.wisp.Link
import org.wisp.utils.lock.*

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext

/**
 * Duplicate `original` stream into links created with `link.copy`
 * Data from `original` is pulled after every link created with `link.copy` is pulled.
 */
class SplitStream[T](original:Link[Operation[T], Operation[T]])(link: SplitStream[T]#Split => Unit)(using ExecutionContext) extends StreamLink[T] {

  protected override val lock:ReentrantLock = new ReentrantLock()

  trait Split {
    def copy: Link[Operation[T], Operation[T]]
  }

  protected class SplitBuilder extends Split {
    var links:List[SplitLink] = Nil

    override def copy: SplitLink = {
      val link = SplitLink()
      links = link :: links
      link
    }

  }

  protected def createNodes(): util.Queue[Link[Operation[T], Operation[T]]] = {
    util.LinkedList[Link[Operation[T], Operation[T]]]()
  }

  protected var requested = true

  protected var ended = false

  protected val nextTo: List[SplitLink] = {
    val s = SplitBuilder()
    link.apply(s)
    s.links
  }

  lock.withLock {
    requested = false
    pullNext()
  }

  override def apply(from: Link[Operation[T], Operation[T]]): PartialFunction[Operation[T], Unit] = {
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

  protected class SplitLink extends StreamLink[T]{

    override protected def lock: ReentrantLock = SplitStream.this.lock

    val nodes: util.Queue[Link[Operation[T], Operation[T]]] = createNodes()

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


    override def apply(from: Link[Operation[T], Operation[T]]): PartialFunction[Operation[T], Unit] = {
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
