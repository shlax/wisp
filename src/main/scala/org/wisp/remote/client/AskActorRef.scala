package org.wisp.remote.client

import org.wisp.bus.UndeliveredMessage
import org.wisp.remote.client.bus.{AlreadyRemoved, ReplacingPath}
import org.wisp.remote.{Connection, RemoteContext}
import org.wisp.remote.codec.{RemoteMessage, RemoteResponse}
import org.wisp.{ActorMessage, ActorRef, AskMessage}

object AskActorRef{

  def process(system: RemoteContext, conn:Connection): PartialFunction[Any, Unit] = {
    case RemoteMessage(path, value, returnTo) =>
      val r = system.get(path)
      r.accept(ActorMessage(new ActorRef(system) {
          override def accept(msg: ActorMessage): Unit = {
            returnTo match {
              case Some(r) =>
                conn.send(RemoteResponse(r, msg.value))
              case None =>
                system.publish(new UndeliveredMessage(msg))
            }
          }
        }, value))
  }

}

class AskActorRef(val path: Any, context:ClientBinding) extends ActorRef(context) {

  override def accept(msg: ActorMessage): Unit = {
    msg match {
      case message: AskMessage =>
        val bm = context.bindMap
        val cf = message.callBack

        val id = context.newBindId()
        val old = bm.put(id, new SenderPath(path, msg.sender, cf))
        cf.whenComplete{ (v, exc) =>
          if(bm.remove(id) == null){
            publish(new AlreadyRemoved(id, v, Option(exc)))
          }
        }
        if(old != null) publish(new ReplacingPath(id, old))

        context.send(RemoteMessage(path, msg.value, Some(id)))
      case _ =>
        context.send(RemoteMessage(path, msg.value, None))
    }
  }

}
