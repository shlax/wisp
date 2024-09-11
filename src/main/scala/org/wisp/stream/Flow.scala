package org.wisp.stream

import org.wisp.ActorRef
import org.wisp.stream.iterator.Source

import java.util
import java.util.function.{Consumer, Function, Predicate}
import scala.annotation.targetName
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

object Flow {

  def apply[T](fn: Consumer[Flow[T]]): Flow[T] = {
    val f = new Flow[T]
    fn.accept(f)
    f
  }

  def apply[T](fe: Source[T])(fn: Consumer[Flow[T]]): Unit = {
    val f = apply(fn)
    try {
      fe.forEach(f)
    }finally {
      f.close()
    }
  }

}

class Flow[T] extends Consumer[T] with AutoCloseable {
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

  def map[R, V >: T](fn: Function[V, R]) : Flow[R] = {
    val nf = new Flow[R]
    to( (e: T) => { nf.accept(fn.apply(e)) } )
    nf
  }

  def flatMap[R, V >: T](fn: Function[Consumer[R], Consumer[V]]) : Flow[R] = {
    val nf = new Flow[R]
    to( (e: T) => { fn.apply(nf).accept(e) } )
    nf
  }

  def groupBy[K, V >: T](keyFn: Function[V, K]): Flow[Seq[T]] = {
    val nf = new Flow[Seq[T]]
    to(new Flow[T]{
      private var queue: mutable.ArrayBuffer[T] = mutable.ArrayBuffer[T]()
      private var key: Option[K] = None

      override def accept(t: T): Unit = {
        val k = keyFn.apply(t)
        if (key.isEmpty) {
          key = Some(k)
        } else if (key.get != k) {
          nf.accept(queue.toSeq)

          key = Some(k)
          queue = mutable.ArrayBuffer[T]()
        }
        queue += t
        super.accept(t)
      }

      override def close(): Unit = {
        if(queue.nonEmpty){
          nf.accept(queue.toSeq)

          key = None
          queue = mutable.ArrayBuffer[T]()
        }
        nf.close()
        super.close()
      }

    })
    nf
  }

  def filter[V >: T](fn: Predicate[V]): Flow[T] = {
    val nf = new Flow[T]
    to( (e: T) => { if(fn.test(e)) nf.accept(e) } )
    nf
  }

  def to[V >: T](s: Consumer[V]): Flow[T] = {
    next.add(s)
    this
  }

  def collect[R, V >: T](pf: PartialFunction[V, R]): Flow[R] = {
    val nf = new Flow[R]
    to((e: T) => {
      if (pf.isDefinedAt(e)) nf.accept(pf.apply(e))
    })
    nf
  }

  def as(fn: Consumer[Flow[T]]): Flow[T] = {
    fn.accept(this)
    this
  }

  @targetName("sendTo")
  def >> (ref:ActorRef): Flow[T] = {
    to( (e: T) => { ref << e } )
  }

}
