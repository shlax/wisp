package org.wisp.stream

import org.wisp.ActorRef
import org.wisp.stream.iterator.Source

import java.util
import java.util.function.{BiConsumer, Consumer, Function, Predicate}
import scala.annotation.targetName
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

object Flow {

  def apply[T](fn: Consumer[Flow[T]]): Flow[T] = {
    val f = new Flow[T]
    fn.accept(f)
    f
  }

  def apply[T](fe: Source[T])(fn: Consumer[Flow[T]]): Flow[T] = {
    val f = apply(fn)
    fe.forEach(f)
    f
  }

}

class Flow[T] extends Consumer[T] {
  private val next = new util.LinkedList[Consumer[_ >: T]]

  override def accept(t: T): Unit = {
    for (i <- next.asScala) i.accept(t)
  }

  def flush(): Unit = {
    for (i <- next.asScala) i match {
      case f: Flow[_] => f.flush()
      case _ =>
    }
  }

  def map[R](fn: Function[_ >: T, R]) : Flow[R] = {
    val nf = new Flow[R]
    add( (e: T) => { nf.accept(fn.apply(e)) } )
    nf
  }

  def flatMap[R](fn: Function[_ >: T, Source[R]]) : Flow[R] = {
    val nf = new Flow[R]
    add( (e: T) => { fn.apply(e).forEach(nf) } )
    nf
  }

  def groupBy[K](keyFn:Function[_ >: T, K]): Flow[Seq[T]] = {
    val nf = new Flow[Seq[T]]
    add(new Flow[T]{
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

      override def flush(): Unit = {
        if(queue.nonEmpty){
          nf.accept(queue.toSeq)

          key = None
          queue = mutable.ArrayBuffer[T]()
        }
        nf.flush()
        super.flush()
      }

    })
    nf
  }

  def filter(fn: Predicate[T]): Flow[T] = {
    val nf = new Flow[T]
    add( (e: T) => { if(fn.test(e)) nf.accept(e) } )
    nf
  }

  def add[R](fn:BiConsumer[T, Flow[R]]): Flow[R] = {
    val nf = new Flow[R]
    add( (e: T) => { fn.accept(e, nf) } )
    nf
  }

  def add(s: Consumer[_ >: T]):Unit = {
    next.add(s)
  }

  def to(fn: T => Unit): Unit = {
    add( (i : T) => { fn.apply(i) } )
  }

  def as(fn: Consumer[Flow[T]]): Flow[T] = {
    fn.accept(this)
    this
  }

  @targetName("sendTo")
  def >> (ref:ActorRef): Unit = {
    add( (e: T) => { ref << e } )
  }

}
