package org.wisp.remote.client

import org.wisp.{ActorMessage, ActorRef}

import java.util.concurrent.CompletableFuture

case class SenderPath(path:Any, sender:ActorRef, callBack:CompletableFuture[ActorMessage])
