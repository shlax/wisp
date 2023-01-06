package org.qwActor.remote.codec

enum MessageType(val size:Int) {
  case objectId extends MessageType(4 * 8)
  case remoteMessage extends MessageType(-1)
  case remoteResponse extends MessageType(-1)
}