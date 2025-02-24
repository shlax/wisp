package org.wisp.stream

import org.wisp.ActorLink
import org.wisp.using.*

import java.util
import java.util.function.{Consumer, Predicate}
import scala.annotation.targetName
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*

object Sink {

  def apply[T](fn: Consumer[Sink[T]]): Sink[T] = {
    val f = new Sink[T]
    fn.accept(f)
    f
  }

  def apply[T](fe: Source[T])(fn: Consumer[Sink[T]]): Unit = {
    apply(fn) | ( f => fe.forEach(f) )
  }

}

class Sink[T] extends Consumer[T] with AutoCloseable {
  private val next = new util.LinkedList[Consumer[? >: T]]

  override def accept(t: T): Unit = {
    for (i <- next.asScala) i.accept(t)
  }

  def close(): Unit = {
    var e:Option[Throwable] = None
    for (i <- next.asScala) i match {
      case f: AutoCloseable =>
        try {
          f.close()
        }catch {
          case NonFatal(ex) =>
            if(e.isDefined) ex.addSuppressed(e.get)
            e = Some(ex)
        }
      case _ =>
    }
    if(e.isDefined){
      throw e.get
    }
  }

  def map[R](fn: T => R) : Sink[R] = {
    val nf = new Sink[R]
    to( (e: T) => { nf.accept(fn.apply(e)) } )
    nf
  }

  def flatMap[R](fn: Consumer[R] => Consumer[T]) : Sink[R] = {
    val nf = new Sink[R]
    to( (e: T) => { fn.apply(nf).accept(e) } )
    nf
  }

  // import scala.jdk.OptionConverters.*
  def groupBy[K, E](keyFn: T => K, collectFn: (Option[E], T) => E): Sink[E] = {
    val nf = new Sink[E]
    to(new Sink[T]{
      private var value: Option[E] = None
      private var key: Option[K] = None

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

      override def close(): Unit = {
        for(x <- value) nf.accept(x)
        value = None
        key = None

        nf.close()
        super.close()
      }

    })
    nf
  }

  def filter[E >: T](fn: Predicate[E]): Sink[T] = {
    val nf = new Sink[T]
    to( (e: T) => { if(fn.test(e)) nf.accept(e) } )
    nf
  }

  def to[E >: T](s: Consumer[E]): Sink[T] = {
    next.add(s)
    this
  }

  def collect[F >: T, R](pf: PartialFunction[F, R]): Sink[R] = {
    val nf = new Sink[R]
    to((e: T) => {
      if (pf.isDefinedAt(e)) nf.accept(pf.apply(e))
    })
    nf
  }

  def as(fn: Consumer[Sink[T]]): Sink[T] = {
    fn.accept(this)
    this
  }

  @targetName("sendTo")
  def >> (ref:ActorLink): Sink[T] = {
    to( (e: T) => { ref << e } )
  }

}
