package org.wisp.stream.iterator

import org.wisp.stream.Source
import java.util
import org.wisp.utils.lock.*

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContextExecutor
import scala.util.control.NonFatal

class RunnableSource[T](src:Source[T])(using ec : ExecutionContextExecutor) 
  extends SourceFlow[T], RunnableStream[T], SingleNodeFlow[T], SynchronizedFlow[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val nodes:util.Queue[Response[T] => Unit] = createNodes()

  protected val condition: Condition = lock.newCondition()
  
  protected var ended = false

  protected var sourceException: Option[Throwable] = None
  
  override def failOn(e:Throwable):this.type = lock.withLock {
    sourceException = Some(e)
    condition.signal()
    this
  }

  override def run():Unit = lock.withLock {

    while (!ended && sourceException.isEmpty){

      var a = nodes.poll()
      while (a != null) {
        var n: Option[T] = None
        if (!ended && sourceException.isEmpty) {
          try {
            n = src.next()
          } catch {
            case NonFatal(ex) =>
              sourceException = Some(ex)
              ec.reportFailure(ex)
          }
        }

        if(ended || sourceException.isDefined){
          a.apply(End)
        }else{
          n match {
            case Some(v) =>
              a.apply(Next(v))
            case None =>
              ended = true
              a.apply(End)
          }
        }

        a = nodes.poll()
      }

      if(!ended && sourceException.isEmpty){
        condition.await()
      }

    }
    
    for(e <- sourceException) throw e
    
  }

  override def nextWithLock(sender: Response[T] => Unit): Unit = {
      if (ended) {
        sender.apply(End)
      } else {
        nodes.add(sender)
        condition.signal()
      }
  }

}
