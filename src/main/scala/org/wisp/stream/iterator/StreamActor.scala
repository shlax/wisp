package org.wisp.stream.iterator

import org.wisp.stream.iterator.message.Operation
import org.wisp.{AbstractActor, ActorScheduler}

abstract class StreamActor(scheduler: ActorScheduler) extends AbstractActor[Operation](scheduler)
