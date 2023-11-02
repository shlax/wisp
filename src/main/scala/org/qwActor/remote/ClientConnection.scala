package org.qwActor.remote

import org.qwActor.remote.client.AskActorRef
import java.nio.channels.AsynchronousSocketChannel

class ClientConnection(system:RemoteSystem, override protected val chanel: AsynchronousSocketChannel) extends AbstractConnection{

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
