package org.wisp.remote.cluster.topology

import java.net.InetSocketAddress
import java.util.concurrent.CompletionException

class ConnectionException(val address:InetSocketAddress, ex: Throwable)
  extends CompletionException("can't connect to: "+address+"("+ex.getMessage+")", ex)
