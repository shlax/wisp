package org.wisp.stream

import org.wisp.ActorRef
import org.wisp.stream.iterator.Source

import java.util
import java.util.function.{BiConsumer, Consumer, Function, Predicate}
import scala.annotation.targetName
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

  def apply(t: T): Unit = {
    for(i <- next.asScala) i.accept(t)
  }

  override def accept(t: T): Unit = apply(t)

  def map[R](fn: Function[_ >: T, R]) : Flow[R] = {
    val nf = new Flow[R]
    to( (e: T) => { nf.accept(fn.apply(e)) } )
    nf
  }

  def flatMap[R](fn: Function[_ >: T, Source[R]]) : Flow[R] = {
    val nf = new Flow[R]
    to( (e: T) => { fn.apply(e).forEach(nf) } )
    nf
  }

  def filter(fn: Predicate[T]): Flow[T] = {
    val nf = new Flow[T]
    to( (e: T) => { if(fn.test(e)) nf.accept(e) } )
    nf
  }

  def add[R](fn:BiConsumer[T, Flow[R]]): Flow[R] = {
    val nf = new Flow[R]
    to( (e: T) => { fn.accept(e, nf) } )
    nf
  }

  def add[C <: Consumer[_ >: T]](s: C): C = {
    to(s)
    s
  }

  def via(fn: Consumer[Flow[T]]): Flow[T] = {
    fn.accept(this)
    this
  }
  
  def to(sink: Consumer[_ >: T]): Unit = {
    next.add(sink)
  }
  
  @targetName("sendTo")
  def >> (ref:ActorRef): Unit = {
    to( (e: T) => { ref << e } )
  }

}
