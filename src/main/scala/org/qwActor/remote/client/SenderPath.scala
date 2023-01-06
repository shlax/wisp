package org.qwActor.remote.client

import org.qwActor.{ActorMessage, ActorRef}

import java.util.concurrent.CompletableFuture

case class SenderPath(path:Any, sender:ActorRef, callBack:CompletableFuture[ActorMessage])
