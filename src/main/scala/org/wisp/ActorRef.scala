package org.wisp

import java.util.function.Consumer

@FunctionalInterface
trait ActorRef extends Consumer[Message]{

}
