package org.wisp.stream

import org.wisp.{ActorLink, Consumer}

import java.util
import scala.annotation.targetName
import scala.concurrent.Promise
import scala.jdk.CollectionConverters.*

object SinkTree {

  def apply[T](fn: SinkTree[T] => Unit): SinkTree[T] = {
    val f = new SinkTree[T]
    fn.apply(f)
    f
  }

  def apply[T, E](fe: Source[T])(fn: SinkTree[T] => E): E = {
    val f = new SinkTree[T]
    val r = fn.apply(f)
    f.forEach(fe)
    r
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

  def fold[E](start:E, collectFn: (E, T) => E): Promise[E] = {
    val p = Promise[E]()
    to(new SinkTree[T]{
      private var value: E = start

      override def accept(t: T): Unit = {
        super.accept(t)
        value = collectFn(value, t)
      }

      override def flush(): Unit = {
        super.flush()
        p.success(value)
      }

    })
    p
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
