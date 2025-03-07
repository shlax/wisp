package org.wisp.stream.typed

import org.wisp.stream.iterator.SourceActorLink

class SourceNode[T](graph: StreamGraph, override val link: SourceActorLink) extends StreamNode[T](graph, link)


