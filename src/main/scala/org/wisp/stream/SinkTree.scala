package org.wisp.stream

import org.wisp.{ActorLink, Consumer}

import java.util
import scala.annotation.targetName
import scala.jdk.CollectionConverters.*

object SinkTree {

  def apply[T](fn: SinkTree[T] => Unit): SinkTree[T] = {
    val f = new SinkTree[T]
    fn.apply(f)
    f
  }

  def apply[T](fe: Source[T])(fn: SinkTree[T] => Unit): Unit = {
    val f = apply(fn)
    f.forEach(fe)
  }

}

class SinkTree[T](val from:Option[SinkTree[?]] = None) extends Sink[T] {
  def this(f:SinkTree[?]) = this(Some(f))

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

  def map[R](fn: T => R) : SinkTree[R] = {
    val nf = new SinkTree[R]
    to(new SinkTree[T](nf){
      override def accept(e: T): Unit = {
        nf.accept(fn.apply(e))
        super.accept(e)
      }
    })
    nf
  }

  def flatMap[R](fn: Sink[R] => Consumer[T]) : SinkTree[R] = {
    val nf = new SinkTree[R]
    to(new SinkTree[T](nf){
      override def accept(e: T): Unit = {
        fn.apply(nf).accept(e)
        super.accept(e)
      }
    })
    nf
  }

  def groupBy[K, E](keyFn: T => K, collectFn: (Option[E], T) => E): SinkTree[E] = {
    val nf = new SinkTree[E]
    to(new SinkTree[T](nf){
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

  def filter[E >: T](fn: E => Boolean): SinkTree[T] = {
    val nf = new SinkTree[T]
    to(new SinkTree[T](nf){
      override def accept(e: T): Unit = {
        if(fn.apply(e)) nf.accept(e)
        super.accept(e)
      }
    })
    nf
  }

  def to[E >: T](s: Sink[E]): SinkTree[T] = {
    next.add(s)
    this
  }

  def collect[F >: T, R](pf: PartialFunction[F, R]): SinkTree[R] = {
    val nf = new SinkTree[R]
    to(new SinkTree[T](nf){
      override def accept(e: T): Unit = {
        if (pf.isDefinedAt(e)) nf.accept(pf.apply(e))
        super.accept(e)
      }
    })
    nf
  }

  def as[R](fn: SinkTree[T] => R): R = {
    fn.apply(this)
  }

  @targetName("sendTo")
  def >> (ref:ActorLink): SinkTree[T] = {
    to( (e: T) => { ref << e } )
  }

}
