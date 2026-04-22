package org.wisp.stream.typed

import org.wisp.stream.iterator.SourceLink

/**
 * Beginning of the stream
 */
class SourceNode[T](graph: StreamGraph, override val link: SourceLink[T]) extends StreamNode[T](graph, link)
