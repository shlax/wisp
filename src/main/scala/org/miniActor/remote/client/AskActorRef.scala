package org.miniActor.remote.client

import org.miniActor.jfr.UndeliverableMessage
import org.miniActor.remote.{Connection, RemoteContext}
import org.miniActor.remote.codec.{RemoteMessage, RemoteResponse}
import org.miniActor.{ActorMessage, ActorRef, AskMessage, logger}

object AskActorRef{

  def process(system: RemoteContext, conn:Connection): PartialFunction[Any, Unit] = {
    case RemoteMessage(path, value, returnTo) =>
      val r = system.get(path)
      r.accept(ActorMessage((msg: ActorMessage) => {
        returnTo match {
          case Some(r) =>
            conn.send(RemoteResponse(r, msg.value))
          case None =>
            val e = new UndeliverableMessage
            if (e.isEnabled && e.shouldCommit) {
              e.message = msg.toString
              e.commit()
            }
            if(logger.isWarnEnabled) logger.warn("dropped message " + msg)
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
            if(logger.isErrorEnabled) {
              if (exc != null) logger.error("nothing to remove " + id + " " + v, exc)
              else logger.error("nothing to remove " + id + " " + v)
            }
          }
        }
        if(old != null && logger.isErrorEnabled) logger.error("replacing "+id+" -> "+old)

        context.send(RemoteMessage(path, msg.value, Some(id)))
      case _ =>
        context.send(RemoteMessage(path, msg.value, None))
    }
  }

}
