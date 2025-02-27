package org.wisp

@FunctionalInterface
trait Consumer[-T] {

  def accept(t:T):Unit

}
