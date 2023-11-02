package org.qwActor.stream.iterator

import org.qwActor.stream.iterator.messages.{End, HasNext, IteratorMessage, Next}
import org.qwActor.{ActorMessage, ActorRef}

import java.util.concurrent.locks.ReentrantLock

object SplitStream {

  def apply(prev: ActorRef): SplitStream = {
    new SplitStream(prev)
  }

}

class SplitStream(prev:ActorRef) extends ActorRef{
  private val lock = new ReentrantLock()
  private var n: List[SplitActorRef] = Nil
  private var ended = false

  private class SplitActorRef extends ActorRef{
    private var requested:Option[ActorRef] = None

    override def accept(t: ActorMessage): Unit = {
      lock.lock()
      try {
        t.value match {
          case HasNext =>
            if(ended){
              t.sender.accept(this, End)
            }else {
              if (requested.isDefined) throw new IllegalStateException("multiple requests")
              requested = Some(t.sender)

              if ( n.forall(_.requested.isDefined) ) {
                prev.ask(HasNext).thenAccept(SplitStream.this)
              }
            }
        }
      } finally {
        lock.unlock()
      }
    }

    def process(): IteratorMessage => Unit = {
      val r = requested.get
      requested = None
      { (m:IteratorMessage) => r.accept(this, m)  }
    }

  }

  def add(): ActorRef = {
    lock.lock()
    try {
      val i = new SplitActorRef()
      n = i :: n
      i
    } finally {
      lock.unlock()
    }
  }

  override def accept(t: ActorMessage): Unit = {
    lock.lock()
    try {
      t.value match {
        case v: Next =>
          val t = n.map(_.process())
          for(i <- t) i.apply(v)

        case End =>
          ended = true
          val t = n.map(_.process())
          for (i <- t) i.apply(End)

      }
    }finally {
      lock.unlock()
    }
  }


}