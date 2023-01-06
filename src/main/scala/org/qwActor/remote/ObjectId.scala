package org.qwActor.remote

import org.qwActor.{ActorMessage, ActorRef}

import java.util.Comparator
import java.util.concurrent.atomic.AtomicLong

object ObjectId extends Ordering[ObjectId]{

  def apply(seriesId:Long, time:Long, seq:Long, random:Long) = new ObjectId(seriesId, time, seq, random)

  def unapply(m: ObjectId): Some[(Long, Long, Long, Long)] = Some((m.seriesId, m.time, m.seq, m.random))

  override def compare(o1: ObjectId, o2: ObjectId): Int = o1.compareTo(o2)
}

@SerialVersionUID(1L)
class ObjectId(val seriesId:Long, val time:Long, val seq:Long, val random:Long) extends Ordered[ObjectId], Serializable {

  override def hashCode(): Int = {
    val hilo = seriesId ^ time ^ seq ^ random
    (hilo >> 32).toInt ^ hilo.toInt
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case id:ObjectId =>
        random == id.random && time == id.time && seq == id.seq && seriesId == id.seriesId
      case _ => false
    }
  }

  override def compare(o: ObjectId): Int = {
    var e = time.compareTo(o.time)
    if(e == 0) e = seriesId.compareTo(o.seriesId)
    if(e == 0) e = seq.compareTo(o.seq)
    if(e == 0) e = random.compareTo(o.random)
    e
  }

  override def toString: String = {
    def hex(l:Long) = java.lang.Long.toUnsignedString(l, java.lang.Character.MAX_RADIX)
    hex(seriesId)+":"+hex(time)+":"+hex(seq)+":"+hex(random)
  }

}
