package org.qwActor.remote.client

import org.qwActor.remote.{Connection, RemoteContext}
import org.qwActor.remote.codec.{RemoteMessage, RemoteResponse}
import org.qwActor.{ActorMessage, ActorRef, AskMessage, logger}

import java.util.concurrent.CompletableFuture

object AskActorRef{

  def process(system: RemoteContext, conn:Connection): PartialFunction[Any, Unit] = {
    case RemoteMessage(path, value, returnTo) =>
      val r = system.get(path)
      r.accept(ActorMessage((msg: ActorMessage) => {
        returnTo match {
          case Some(r) =>
            conn.send(RemoteResponse(r, msg.value))
          case None =>
            logger.warn("dropped message " + msg)
        }
      }, value))
  }

}

class AskActorRef(val path: Any, context:ClientBinding) extends ActorRef {

  override def accept(msg: ActorMessage): Unit = {
    msg match {
      case message: AskMessage =>
        val bm = context.bindMap
        val cf = message.callBack

        val id = context.newBindId()
        val old = bm.put(id, SenderPath(path, msg.sender, cf))
        cf.whenComplete{ (v, exc) =>
          if(bm.remove(id) == null){
            if(exc != null) logger.error("nothing to remove "+id+" "+v, exc)
            else logger.error("nothing to remove "+id+" "+v)
          }
        }
        if(old != null) logger.error("replacing "+id+" -> "+old)

        context.send(RemoteMessage(path, msg.value, Some(id)))
      case _ =>
        context.send(RemoteMessage(path, msg.value, None))
    }
  }

}
