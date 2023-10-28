package org.qwActor.stream.iterator

import org.qwActor.stream.iterator.messages.{End, HasNext, IteratorMessage, Next}
import org.qwActor.{Actor, ActorContext, ActorMessage, ActorRef}

import java.util.concurrent.locks.ReentrantLock

object SplitActor {

  def apply(prev: ActorRef): SplitActor = {
    new SplitActor(prev)
  }

}

class SplitActor(prev:ActorRef) extends ActorRef{
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

              if (n.forall(_.requested.isDefined)) {
                prev.accept(SplitActor.this, HasNext)
              }
            }
        }
      } finally {
        lock.unlock()
      }
    }

    def process(v:IteratorMessage): Unit = {
      requested.get.accept(this, v)
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
          for (i <- n) i.process(v)

        case End =>
          ended = true
          for (i <- n) i.process(End)

      }
    }finally {
      lock.unlock()
    }
  }


}