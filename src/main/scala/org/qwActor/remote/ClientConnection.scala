package org.qwActor.remote

import org.qwActor.remote.client.AskActorRef
import org.qwActor.{ActorMessage, ActorRef, logger}
import org.qwActor.remote.codec.{ByteArrayDecoder, ByteArrayEncoder, Decoder, Encoder, RemoteMessage, RemoteResponse}

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousSocketChannel, CompletionHandler}
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

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
