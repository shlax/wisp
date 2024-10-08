package org.wisp.remote

import org.wisp.remote.client.AskActorRef
import java.nio.channels.AsynchronousSocketChannel

class ClientConnection(system:RemoteSystem, override protected val chanel: AsynchronousSocketChannel) extends AbstractConnection(system){

  override def process: PartialFunction[Any, Unit] = AskActorRef.process(system, this)

  override def startReading(): Unit = {
    super.startReading()
    send(system.id)
  }

  override def close(): Unit ={
    system.close(this)
    super.close()
  }
}
