package org.wisp.stream

import org.wisp.{Link, Message}

package object iterator {

  type OperationLink[T] = Link[Operation[T], Operation[T]]

  type OperationMessage[T] = Message[Operation[T], Operation[T]]

}
