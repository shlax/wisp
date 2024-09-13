package org.wisp.remote

import org.wisp.bus.EventBus

import java.util.concurrent.CompletableFuture

trait Connection extends EventBus, AutoCloseable{

  def send(msg: Any): Unit

  def disconnect(): CompletableFuture[Void]

}
