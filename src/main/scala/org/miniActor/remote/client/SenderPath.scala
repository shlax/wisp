package org.miniActor.remote.client

import org.miniActor.{ActorMessage, ActorRef}

import java.util.concurrent.CompletableFuture

case class SenderPath(path:Any, sender:ActorRef, callBack:CompletableFuture[ActorMessage])
