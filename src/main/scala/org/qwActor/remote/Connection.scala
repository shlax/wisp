package org.qwActor.remote

import java.util.concurrent.CompletableFuture

trait Connection extends AutoCloseable{

  def send(msg: Any): Unit

  def disconnect(): CompletableFuture[Void]

}
