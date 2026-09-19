package org.wisp.utils

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

object uuid {

  private val counter = AtomicInteger(ThreadLocalRandom.current().nextInt(4096))

  /**
   * generate random UUID using ThreadLocalRandom
   */
  def generateUUID():UUID = {

    val acc = counter.getAndUpdate{ i =>
      val j = i + 1
      if(j >= 4096) 0 else j
    }

    // mask to ensure it fits in 12 bits
    var mostSigBits = ( System.currentTimeMillis() << 16 ) | ( acc & 0xFFFL )
    var leastSigBits = ThreadLocalRandom.current().nextLong()

    // Set version to 7 (0111 in bits 48-51)
    mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L

    // Set variant to 10 (RFC 4122) in bits 64-65
    leastSigBits = (leastSigBits & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L

    new UUID(mostSigBits, leastSigBits)
  }

}
