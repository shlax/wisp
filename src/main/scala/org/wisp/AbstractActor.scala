package org.wisp

/**
 * [[Actor]] that takes [[ActorScheduler]] as constructor argument
 */
abstract class AbstractActor[T](override protected val scheduler: ActorScheduler[T]) extends Actor[T]
