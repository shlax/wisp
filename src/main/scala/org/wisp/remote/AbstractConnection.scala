package org.wisp.remote

import org.wisp.bus.EventBus
import org.wisp.remote.bus.{FailedClose, FailedConnection, FailedDisconnect, FailedRemoteMessage, UndeliveredRemoteMessage}
import org.wisp.MessageQueue
import org.wisp.remote.codec.{ByteArrayDecoder, ByteArrayEncoder, Decoder, Disconnect, Encoder}

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousCloseException, AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.collection.mutable
import scala.util.control.NonFatal

object AbstractConnection {

  def disconnect(map: java.util.Map[?, ? <: AbstractConnection]): CompletableFuture[Void] = {
    val l = new mutable.ArrayBuffer[CompletableFuture[Void]](map.size())
    map.forEach { (_, v) =>
      l += v.disconnect().whenComplete { (_, exc) =>
        if (exc != null) v.publish(new FailedDisconnect(v, exc))
      }
    }

    val a = l.toArray[CompletableFuture[?]]
    CompletableFuture.allOf(a*)
  }

}

abstract class AbstractConnection(val eventBus: EventBus) extends Connection, CompletionHandler[Integer, Attachment], Consumer[Any] {
  override def publish(event: Any): Unit = eventBus.publish(event)

  protected def chanel: AsynchronousSocketChannel

  protected def allocateReadBuffer(): ByteBuffer = ByteBuffer.allocateDirect(1024)
  private val readBuffer = allocateReadBuffer()

  protected def allocateWriteBuffer(): ByteBuffer = ByteBuffer.allocateDirect(1024)
  private val writeBuffer = allocateWriteBuffer()

  protected def createQueue(): MessageQueue[Any] = MessageQueue()
  private val queue = createQueue()

  protected def encoderFor(msg: Any): Encoder = new ByteArrayEncoder(msg)

  protected def createDecoder(): Decoder = new ByteArrayDecoder(this)
  private val decoder = createDecoder()


  protected val lock = new ReentrantLock()
  protected val condition = lock.newCondition()

  private var sentDisconnect: Boolean = false

  private var encoder: Option[Encoder] = None
  private var writing: Boolean = false

  override def send(msg: Any): Unit = {
    lock.lock()
    try {
      doSend(msg)
    }finally {
      lock.unlock()
    }
  }

  /** call inside lock/condition */
  protected def doSend(msg: Any): Unit = {
    if(disconnected.isDone) throw new AsynchronousCloseException()
    addToQueue(msg)
  }

  /** call inside lock/condition */
  private def addToQueue(msg: Any): Unit = {
    if (msg == Disconnect || msg.isInstanceOf[ObjectId]) {
      queue.add(msg)
    } else {
      while (!queue.put(msg)) {
        condition.await()
      }
    }
    startWrite()
  }

  protected def startReading():Unit = {
    chanel.read(readBuffer, Attachment.Read, this)
  }

  /** call inside lock/condition */
  private def startWrite(): Unit = {
    if(sentDisconnect){
      var tmp = queue.poll()
      while (tmp != null) {
        publish(new UndeliveredRemoteMessage(this, tmp))
        tmp = queue.poll()
      }
      condition.signalAll()
    }else if (!writing) {
      encoder match {
        case Some(enc) =>
          enc.write(writeBuffer) match {
            case Some(done) =>
              if (done) encoder = None
            case None =>
              encoder = None
              sentDisconnect = true
          }

          writing = true
          writeBuffer.flip()
          chanel.write(writeBuffer, Attachment.Write, this)
        case None =>
          val msg = queue.poll()
          condition.signalAll()
          if (msg != null) {
            val enc = encoderFor(msg)
            enc.write(writeBuffer) match {
              case Some(done) =>
                if(!done) encoder = Some(enc)
              case None =>
                sentDisconnect = true
            }

            writing = true
            writeBuffer.flip()
            chanel.write(writeBuffer, Attachment.Write, this)
          }
      }
    }
  }

  protected def process: PartialFunction[Any, Unit]

  override def accept(t: Any): Unit = {
    try {
      process.apply(t)
    } catch {
      case NonFatal(exc) =>
        publish(new FailedRemoteMessage(this, t, exc))
    }
  }

  protected val writeDisconnect = new CompletableFuture[Void]()
  protected val readDisconnect = new CompletableFuture[Void]()

  protected val disconnected : CompletableFuture[Void] = CompletableFuture.allOf(readDisconnect, writeDisconnect).whenComplete{ (_, exc) =>
    if(exc != null) publish(new FailedDisconnect(AbstractConnection.this, exc))
    close()
  }

  override def disconnect(): CompletableFuture[Void] = {
    send(Disconnect)
    disconnected
  }

  override def close(): Unit = {
    try {
      chanel.close()
    }catch{
      case NonFatal(e) =>
        publish(new FailedClose(this, e))
    }finally {
      readDisconnect.completeExceptionally(new AsynchronousCloseException)
      writeDisconnect.completeExceptionally(new AsynchronousCloseException)
    }
  }

  override def completed(result: Integer, attachment: Attachment): Unit = {
    attachment match {
      case Attachment.Read =>
        if (result.intValue() == -1) {
          close()
        } else {
          readBuffer.flip()
          if( decoder.read(readBuffer) ){
            readBuffer.clear()
            chanel.read(readBuffer, Attachment.Read, this)
          }else{
            readDisconnect.complete(null)
            lock.lock()
            try {
              if(!sentDisconnect) addToQueue(Disconnect)
            }finally {
              lock.unlock()
            }
          }
        }
      case Attachment.Write =>
        lock.lock()
        try{
          if(sentDisconnect) writeDisconnect.complete(null)
          writeBuffer.clear()
          writing = false
          startWrite()
        }finally {
          lock.unlock()
        }
    }
  }

  override def failed(exc: Throwable, attachment: Attachment): Unit = {
    try {
      close()
    }finally {
      publish(new FailedConnection(this, exc))
    }
  }

}
