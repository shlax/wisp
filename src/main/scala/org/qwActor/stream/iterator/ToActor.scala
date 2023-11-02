package org.qwActor.stream.iterator

import org.qwActor.{Actor, ActorContext, ActorMessage, ActorRef}

object ToActor{

  def apply(ref:ActorRef, context: ActorContext): Actor = new Actor(context){
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case v => ref.accept(new ActorMessage(sender, v))
    }
  }

}
