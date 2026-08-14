package org.wisp.stream.graph

import org.wisp.stream.iterator.SourceFlow

/**
 * Beginning of the stream
 */
class SourceNode[T](graph: StreamGraph, override val link: SourceFlow[T]) extends StreamNode[T](graph, link)
