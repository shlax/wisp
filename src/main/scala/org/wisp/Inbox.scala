package org.wisp

import scala.concurrent.ExecutionContext

trait Inbox {
  
  def add(message: Message):Unit

}
