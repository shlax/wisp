package org.wisp.stream.graph

import org.wisp.stream.iterator.SourceLink

/**
 * Beginning of the stream
 */
class SourceNode[T](graph: StreamGraph, override val link: SourceLink[T]) extends StreamNode[T](graph, link)
