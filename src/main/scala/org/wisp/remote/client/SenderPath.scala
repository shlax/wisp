package org.wisp.remote.client

import org.wisp.{ActorMessage, ActorRef}

import java.util.concurrent.CompletableFuture

class SenderPath(val path:Any, val sender:ActorRef, val callBack:CompletableFuture[ActorMessage])
