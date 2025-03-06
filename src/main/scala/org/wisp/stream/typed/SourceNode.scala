package org.wisp.stream.typed

import org.wisp.stream.iterator.SourceActorLink

import scala.concurrent.ExecutionContext

class SourceNode[T](graph: StreamGraph, override val link: SourceActorLink) extends StreamNode[T](graph, link)


