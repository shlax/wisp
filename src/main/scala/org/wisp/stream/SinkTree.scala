package org.wisp.stream

import org.wisp.{ActorLink, Consumer}

import java.util
import scala.annotation.targetName
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.*

object SinkTree {

  def apply[T, E](fn: SinkTree[T] => E): (Sink[T], E) = {
    val f = new SinkTree[T]()
    val g = fn.apply(f)
    (f.build(), g)
  }

  def apply[T](fn: SinkTree[T] => Unit): Sink[T] = {
    val f = new SinkTree[T]()
    fn.apply(f)
    f.build()
  }

  def apply[T, E](source: Source[T])(fn: SinkTree[T] => E): E = {
    val f = new SinkTree[T]()
    val r = fn.apply(f)
    source.forEach(f.build())
    r
  }

}

/** Common operations for `Sink` */
class SinkTree[T] extends Sink[T] {

  protected val next = new util.LinkedList[Sink[? >: T]]

  /** optimize internal structure */
  protected def build(): Sink[T] = {
    if(next.isEmpty) throw new IllegalStateException("empty")
    for(i <- 0 until next.size()){
      next.get(i) match{
        case t : SinkTree[? >: T] =>
          val b = t.build()
          if(t != b) next.set(i, b)
        case _ =>
      }
    }
    if(next.size() == 1) next.getFirst else this
  }

  override def accept(t: T): Unit = {
    for (i <- next.asScala) i.accept(t)
  }

  override def flush(): Unit = {
    for(i <- next.asScala) i.flush()
  }

  def map[R](fn: T => R) : SinkTree[R] = {
    val nf = new SinkTree[R]
    to(new Sink[T](){
      override def accept(e: T): Unit = {
        nf.accept(fn.apply(e))
      }
      override def flush(): Unit = {
        nf.flush()
      }
    })
    nf
  }

  def flatMap[R](fn: Sink[R] => Consumer[T]) : SinkTree[R] = {
    val nf = new SinkTree[R]
    to(new Sink[T](){
      override def accept(e: T): Unit = {
        fn.apply(nf).accept(e)
      }
      override def flush(): Unit = {
        nf.flush()
      }
    })
    nf
  }

  def filter[E >: T](fn: E => Boolean): SinkTree[T] = {
    val nf = new SinkTree[T]
    to(new Sink[T](){
      override def accept(e: T): Unit = {
        if(fn.apply(e)) nf.accept(e)
      }
      override def flush(): Unit = {
        nf.flush()
      }
    })
    nf
  }

  def collect[F >: T, R](pf: PartialFunction[F, R]): SinkTree[R] = {
    val nf = new SinkTree[R]
    to(new Sink[T](){
      override def accept(e: T): Unit = {
        if (pf.isDefinedAt(e)) nf.accept(pf.apply(e))
      }
      override def flush(): Unit = {
        nf.flush()
      }
    })
    nf
  }

  def fold[E](start:E)(collectFn: (E, T) => E): Future[E] = {
    val p = Promise[E]()
    to(new Sink[T](){
      private var value: E = start

      override def accept(t: T): Unit = {
        value = collectFn(value, t)
      }

      override def flush(): Unit = {
        p.success(value)
      }

    })
    p.future
  }

  def to[E >: T](s: Sink[E]): SinkTree[T] = {
    next.add(s)
    this
  }

  def as[R](fn: this.type => R): R = {
    fn.apply(this)
  }

  @targetName("sendTo")
  def >> (ref:ActorLink): SinkTree[T] = {
    to( (e: T) => { ref << e } )
  }

}
