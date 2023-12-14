package org.wisp.remote.cluster.topology

case class Connection[T](from: T, to: T) {
  override def toString: String = "Connection(" + from + "=>" + to + ")"
}
