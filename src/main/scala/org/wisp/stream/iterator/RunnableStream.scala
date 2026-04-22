package org.wisp.stream.iterator

import scala.concurrent.{ExecutionContext, Future}

trait RunnableStream[T] extends StreamActorLink[T], Runnable {

  def start(using ExecutionContext): Future[Unit] = {
    Future { run() }
  }

}
