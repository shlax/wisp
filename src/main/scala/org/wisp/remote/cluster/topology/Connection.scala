package org.wisp.remote.cluster.topology

class Connection[T](val from: T, val to: T) {
  override def toString: String = "Connection(" + from + "=>" + to + ")"
}
