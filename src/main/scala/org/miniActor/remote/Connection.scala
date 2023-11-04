package org.miniActor.remote

import java.util.concurrent.CompletableFuture

trait Connection extends AutoCloseable{

  def send(msg: Any): Unit

  def disconnect(): CompletableFuture[Void]

}
