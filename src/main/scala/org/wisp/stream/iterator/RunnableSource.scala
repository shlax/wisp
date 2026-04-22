package org.wisp.stream.iterator

import org.wisp.stream.Source
import java.util
import org.wisp.utils.lock.*

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class RunnableSource[T](src:Source[T])(using ec : ExecutionContext) extends SourceLink[T], RunnableStream[T], SingleNodeFlow[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val nodes:util.Queue[OperationLink[T]] = createNodes()

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
          a << End
        }else{
          n match {
            case Some(v) =>
              a << Next(v)
            case None =>
              ended = true
              a << End
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

  override def apply(sender: OperationLink[T]): PartialFunction[Operation[T], Unit] = {
    case HasNext =>
      if (ended) {
        sender << End
      } else {
        nodes.add(sender)
        condition.signal()
      }
  }

}
