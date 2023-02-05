package org.qwActor.jfr

import jdk.jfr.{Description, Event, Label}
import org.qwActor.ActorMessage

@Label("Undeliverable message")
@Description("Undeliverable message")
class UndeliverableMessage extends Event{

  @Label("actor message")
  var message:String = ""

}
