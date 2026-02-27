package org.wisp.stream.iterator

import scala.concurrent.{ExecutionContext, Future}

trait RunnableStream extends StreamConsumer, Runnable {

  def start(using ExecutionContext): Future[Unit] = {
    Future { run() }
  }

}
