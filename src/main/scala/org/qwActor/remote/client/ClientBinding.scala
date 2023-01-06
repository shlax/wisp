package org.qwActor.remote.client

import org.qwActor.{ActorMessage, ActorRef, logger}
import org.qwActor.remote.Connection
import org.qwActor.remote.codec.{RemoteMessage, RemoteResponse}
import org.qwActor.remote.{AbstractConnection, ObjectId, ObjectIdFactory, RemoteContext}

import java.nio.channels.AsynchronousCloseException
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import scala.jdk.CollectionConverters.*

object ClientBinding{

  def process(cb: ClientBinding): PartialFunction[Any, Unit] = {
    case uuid: ObjectId =>
      cb.onConnected(uuid)
    case m: RemoteResponse =>
      cb.processRemoteResponse(m)
  }

}

trait ClientBinding extends RemoteContext, Connection{

  protected def createBindMap(): ConcurrentMap[ObjectId, SenderPath] = new ConcurrentHashMap[ObjectId, SenderPath]()
  def bindMap : ConcurrentMap[ObjectId, SenderPath]

  def newBindId(): ObjectId

  def onConnected(uuid:ObjectId):Unit

  def processRemoteResponse(msg:RemoteResponse):Unit = {
    val r = bindMap.get(msg.returnTo)
    if (r != null) {
      r.sender.accept(get(r.path), msg.value)
    } else {
      logger.warn("dropped message " + msg)
    }
  }

  override def get(path: Any): AskActorRef = {
    new AskActorRef(path, this)
  }

  override def close(): Unit = {
    var ex : Option[AsynchronousCloseException] = None
    for (e <- bindMap.values().asScala){
      val exc = ex match {
        case Some(v) => v
        case None =>
          val n = new AsynchronousCloseException
          ex = Some(n)
          n
      }
      e.callBack.completeExceptionally(exc)
    }
  }

}
