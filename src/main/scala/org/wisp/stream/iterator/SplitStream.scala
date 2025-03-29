package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.lock.*
import org.wisp.stream.iterator.message.{HasNext, Next, End}

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext

/** Duplicate `original` stream into links created with `link.next()`
 * Data from `original` is pulled after every link created with `link.next()` is pulled.
 * @note elements are not duplicated */
class SplitStream(original:ActorLink)(link: SplitStream#Split => Unit)(using ExecutionContext) extends StreamConsumer {

  trait Split {
    def next(): ActorLink
  }

  protected class SplitBuilder extends Split {
    var links:List[SplitActorLink] = Nil

    override def next(): SplitActorLink = {
      val link = SplitActorLink()
      links = link :: links
      link
    }

  }

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  protected val lock = new ReentrantLock()
  protected var requested:Boolean = false
  protected var exception:Option[Throwable] = None
  protected var ended = false

  protected val nextTo: List[SplitActorLink] = {
    val s = SplitBuilder()
    link.apply(s)
    s.links
  }

  override def accept(t:Message):Unit = lock.withLock {
    if(!requested) throw new IllegalStateException("not requested")
    requested = false

    t.value match {
      case Next(v) =>
        for(n <- nextTo) n.next(v)
        pullNext()
      case End(v) =>
        if(v.isDefined){
          exception = v
        }else{
          ended = true
        }

        for(n <- nextTo) n.end()
    }
  }

  protected def pullNext():Unit = {
    if(!requested && nextTo.forall(i => !i.nodes.isEmpty)){
      requested = true
      original.call(HasNext).onComplete(SplitStream.this.accept)
    }
  }

  protected class SplitActorLink extends ActorLink{
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
        n << End(exception)
        n = nodes.poll()
      }
    }

    override def accept(t: Message): Unit = lock.withLock {
      t.value match {
        case HasNext =>
          if(exception.isDefined || ended){
            t.sender << End(exception)
          }else{
            nodes.add(t.sender)
            pullNext()
          }
      }
    }
  }

}
