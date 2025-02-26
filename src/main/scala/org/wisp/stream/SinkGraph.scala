package org.wisp.stream

import org.wisp.ActorLink
import org.wisp.using.*

import java.util
import java.util.function.{Consumer, Predicate}
import scala.annotation.targetName
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*

object SinkGraph {

  def apply[T](fn: Consumer[SinkGraph[T]]): SinkGraph[T] = {
    val f = new SinkGraph[T]
    fn.accept(f)
    f
  }

  def apply[T](fe: Source[T])(fn: Consumer[SinkGraph[T]]): Unit = {
    apply(fn) | { f => f.forEach(fe) }
  }

}

class SinkGraph[T](val from:Option[SinkGraph[?]] = None) extends Sink[T] {
  def this(f:SinkGraph[?]) = this(Some(f))

  protected val next = new util.LinkedList[Sink[? >: T]]

  override def accept(t: T): Unit = {
    for (i <- next.asScala) i.accept(t)
  }

  def forEach(fe: Source[T]):Unit = {
    fe.forEach(this)
    flush()
  }

  override def flush(): Unit = {
    for(f <- from) f.flush()
    for(i <- next.asScala) i.flush()
  }

  override def close(): Unit = {
    var e:Throwable = null

    for(i <- from){
      try{
        i.close()
      }catch{
        case NonFatal(ex) =>
          e = ex
      }
    }

    for (i <- next.asScala){
      try {
        i.close()
      }catch {
        case NonFatal(ex) =>
          if(e != null) e.addSuppressed(ex)
          else e = ex
      }
    }

    if(e != null){
      throw e
    }

  }

  def map[R](fn: T => R) : SinkGraph[R] = {
    val nf = new SinkGraph[R]
    to(new SinkGraph[T](nf){
      override def accept(e: T): Unit = {
        nf.accept(fn.apply(e))
        super.accept(e)
      }
    })
    nf
  }

  def flatMap[R](fn: Consumer[R] => Consumer[T]) : SinkGraph[R] = {
    val nf = new SinkGraph[R]
    to(new SinkGraph[T](nf){
      override def accept(e: T): Unit = {
        fn.apply(nf).accept(e)
        super.accept(e)
      }
    })
    nf
  }

  // import scala.jdk.OptionConverters.*
  def groupBy[K, E](keyFn: T => K, collectFn: (Option[E], T) => E): SinkGraph[E] = {
    val nf = new SinkGraph[E]
    to(new SinkGraph[T](nf){
      protected var value: Option[E] = None
      protected var key: Option[K] = None

      override def accept(t: T): Unit = {
        val k = keyFn.apply(t)
        if (key.isEmpty) {
          value = Option(collectFn.apply(value, t))
          key = Some(k)
        } else if (key.get != k) {
          for(x <- value) nf.accept(x)
          value = Option(collectFn.apply(None, t))
          key = Some(k)
        }else{
          value = Option(collectFn.apply(value, t))
        }
        super.accept(t)
      }

      override def flush(): Unit = {
        for (x <- value) nf.accept(x)
        value = None
        key = None
        super.flush()
      }

    })
    nf
  }

  def filter[E >: T](fn: Predicate[E]): SinkGraph[T] = {
    val nf = new SinkGraph[T]
    to(new SinkGraph[T](nf){
      override def accept(e: T): Unit = {
        if(fn.test(e)) nf.accept(e)
        super.accept(e)
      }
    })
    nf
  }

  def to[E >: T](s: Sink[E]): SinkGraph[T] = {
    next.add(s)
    this
  }

  def collect[F >: T, R](pf: PartialFunction[F, R]): SinkGraph[R] = {
    val nf = new SinkGraph[R]
    to(new SinkGraph[T](nf){
      override def accept(e: T): Unit = {
        if (pf.isDefinedAt(e)) nf.accept(pf.apply(e))
        super.accept(e)
      }
    })
    nf
  }

  def as[R](fn: SinkGraph[T] => R): R = {
    fn.apply(this)
  }

  @targetName("sendTo")
  def >> (ref:ActorLink): SinkGraph[T] = {
    to( (e: T) => { ref << e } )
  }

}
