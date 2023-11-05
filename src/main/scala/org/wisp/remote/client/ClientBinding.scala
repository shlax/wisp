package org.wisp.remote.client

import org.wisp.jfr.UndeliverableMessage
import org.wisp.logger
import org.wisp.remote.Connection
import org.wisp.remote.codec.RemoteResponse
import org.wisp.remote.{ObjectId, RemoteContext}

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
      val e = new UndeliverableMessage
      if (e.isEnabled && e.shouldCommit) {
        e.message = msg.toString
        e.commit()
      }
      if(logger.isWarnEnabled) logger.warn("dropped message " + msg)
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
