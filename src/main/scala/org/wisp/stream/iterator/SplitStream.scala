package org.wisp.stream.iterator

import org.wisp.utils.lock.*

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContextExecutor

/**
 * Duplicate `original` stream into links created with `link.copy`
 * Data from `original` is pulled after every link created with `link.copy` is pulled.
 */
class SplitStream[T](original:StreamFlow[T])(link: SplitStream[T]#Split => Unit)(using ExecutionContextExecutor) extends StreamLink[T] {

  protected override val lock:ReentrantLock = new ReentrantLock()

  trait Split {
    def copy: StreamFlow[T]
  }

  protected class SplitBuilder extends Split {
    var links:List[SplitLink] = Nil

    override def copy: SplitLink = {
      val link = SplitLink()
      links = link :: links
      link
    }

  }

  protected def createNodes(): util.Queue[Response[T] => Unit] = {
    util.LinkedList[Response[T] => Unit]()
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

  override def applyWithLock(rv:Response[T]): Unit = rv match {
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
      original.next(SplitStream.this)
    }
  }

  protected class SplitLink extends SynchronizedFlow[T]{
    override protected def lock: ReentrantLock = SplitStream.this.lock

    val nodes: util.Queue[Response[T] => Unit] = createNodes()

    def next(v:T): Unit = {
      val n = nodes.poll()
      if(n == null){
        throw new IllegalStateException("nodes are empty")
      }
      n.apply(Next(v))
    }

    def end(): Unit = {
      var n = nodes.poll()
      if(n == null) throw new IllegalStateException("nodes are empty")
      while(n != null){
        n.apply(End)
        n = nodes.poll()
      }
    }

    override def nextWithLock(from: Response[T] => Unit): Unit = {
      if (ended) {
        from.apply(End)
      } else {
        nodes.add(from)
        pullNext()
      }
    }

  }

}
