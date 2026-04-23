package org.wisp.remote

/**
 * Base trait for remote messages that carry routing information.
 */
trait RemoteMessage[K] {

  /**
   * Returns the routing path for this message.
   */
  def path : K

}
