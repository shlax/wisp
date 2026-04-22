package org.wisp

/**
 * [[Actor]] that takes [[ActorScheduler]] as constructor argument
 */
abstract class AbstractActor[-T, +R](override protected val scheduler: ActorScheduler[T, R]) extends Actor[T, R]
