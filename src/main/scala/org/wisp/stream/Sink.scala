package org.wisp.stream

import org.wisp.ActorLink
import org.wisp.using.*

import java.io.Flushable
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
    apply(fn) | { f => f.forEach(fe) }
  }

}

class Sink[T](val from:Option[Sink[?]] = None) extends Consumer[T], Flushable, AutoCloseable {
  def this(f:Sink[?]) = this(Some(f))

  protected val next = new util.LinkedList[Consumer[? >: T]]

  override def accept(t: T): Unit = {
    for (i <- next.asScala) i.accept(t)
  }

  def forEach(fe: Source[T]):Unit = {
    fe.forEach(this)
    flush()
  }

  override def flush(): Unit = {
    for(f <- from) f.flush()
    for(i <- next.asScala) i match{
      case f:Flushable =>
        f.flush()
      case _ =>
    }
  }

  def close(): Unit = {
    var e:Throwable = null
    for(i <- from){
      try{
        i.close()
      }catch{
        case NonFatal(ex) =>
          e = ex
      }
    }
    for (i <- next.asScala) i match {
      case f: AutoCloseable =>
        try {
          f.close()
        }catch {
          case NonFatal(ex) =>
            if(e != null) e.addSuppressed(ex)
            else e = ex
        }
      case _ =>
    }
    if(e != null){
      throw e
    }
  }

  def map[R](fn: T => R) : Sink[R] = {
    val nf = new Sink[R]
    to(new Sink[T](nf){
      override def accept(e: T): Unit = {
        nf.accept(fn.apply(e))
        super.accept(e)
      }
    })
    nf
  }

  def flatMap[R](fn: Consumer[R] => Consumer[T]) : Sink[R] = {
    val nf = new Sink[R]
    to(new Sink[T](nf){
      override def accept(e: T): Unit = {
        fn.apply(nf).accept(e)
        super.accept(e)
      }
    })
    nf
  }

  // import scala.jdk.OptionConverters.*
  def groupBy[K, E](keyFn: T => K, collectFn: (Option[E], T) => E): Sink[E] = {
    val nf = new Sink[E]
    to(new Sink[T](nf){
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

  def filter[E >: T](fn: Predicate[E]): Sink[T] = {
    val nf = new Sink[T]
    to(new Sink[T](nf){
      override def accept(e: T): Unit = {
        if(fn.test(e)) nf.accept(e)
        super.accept(e)
      }
    })
    nf
  }

  def to[E >: T](s: Consumer[E]): Sink[T] = {
    next.add(s)
    this
  }

  def collect[F >: T, R](pf: PartialFunction[F, R]): Sink[R] = {
    val nf = new Sink[R]
    to(new Sink[T](nf){
      override def accept(e: T): Unit = {
        if (pf.isDefinedAt(e)) nf.accept(pf.apply(e))
        super.accept(e)
      }
    })
    nf
  }

  def as[R](fn: Sink[T] => R): R = {
    fn.apply(this)
  }

  @targetName("sendTo")
  def >> (ref:ActorLink): Sink[T] = {
    to( (e: T) => { ref << e } )
  }

}
