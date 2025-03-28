package org.wisp

/** [[Actor]] that takes [[ActorScheduler]] as constructor argument */
abstract class AbstractActor(override protected val scheduler: ActorScheduler) extends Actor
