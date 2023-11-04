package org.miniActor.jfr

import jdk.jfr.{Description, Event, Label}
import org.miniActor.ActorMessage

@Label("Undeliverable message")
@Description("Undeliverable message")
class UndeliverableMessage extends Event{

  @Label("actor message")
  var message:String = ""

}
