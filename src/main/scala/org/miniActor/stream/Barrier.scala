package org.miniActor.stream

import org.miniActor.ActorRef

import java.util.function.Consumer

trait Barrier[T] extends Consumer[T] {

  def nextTo(r:Consumer[T]):Unit

  def next(r: ActorRef): Unit = {
    nextTo((t: T) => { r << t })
  }

}
