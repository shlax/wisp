package org.wisp.utils

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

object uuid {

  /**
   * generate random UUID using ThreadLocalRandom
   */
  def generate():UUID = {
    val timestamp = System.currentTimeMillis()
    val random = ThreadLocalRandom.current()

    // mask to ensure it fits in 12 bits
    var mostSigBits = (timestamp << 16) | (random.nextInt(4096) & 0xFFFL)
    var leastSigBits = random.nextLong()

    // Set version to 7 (0111 in bits 48-51)
    mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L

    // Set variant to 10 (RFC 4122) in bits 64-65
    leastSigBits = (leastSigBits & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L

    new UUID(mostSigBits, leastSigBits)
  }

}
