package org.qwActor.remote

import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicLong

object ObjectIdFactory{

  val random = new SecureRandom()

  def seriesId():Long = {

    def find(fn: NetworkInterface => Boolean ) = {
      var mac: Option[Array[Byte]] = None
      NetworkInterface.networkInterfaces().forEach { i =>
        val m = i.getHardwareAddress
        if(m != null && fn(i)) mac = Some(m)
      }
      mac
    }

    var mac = find(_.isUp)
    if(mac.isEmpty) mac = find( _ => true )

    if(mac.isEmpty) random.nextLong()
    else{
      val adr = mac.get
      val rand = new Array[Byte](2)
      random.nextBytes(rand)

      val data = new Array[Byte](8)
      for(i <- 0 until 6) data(i) = adr(i)
      for(i <- 0 until 2) data(i+6) = rand(i)

      var l:Long = 0L
      for (i <- 0 until 8) l = (l << 8) | ( data(i) & 0xff )

      l
    }
  }

  def apply() : Callable[ObjectId] = {
    new Callable[ObjectId]{
      val series:Long = seriesId()
      val seq = new AtomicLong(random.nextLong())
      override def call(): ObjectId = ObjectId(series, System.currentTimeMillis(), seq.getAndIncrement(), random.nextLong())
    }
  }

}

trait ObjectIdFactory {

  protected def createObjectIdFactory(): Callable[ObjectId] = ObjectIdFactory()
  def objectIdFactory: Callable[ObjectId]

  def newObjectId(): ObjectId = {
    objectIdFactory.call()
  }

}
