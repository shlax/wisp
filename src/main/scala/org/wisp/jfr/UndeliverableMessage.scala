package org.wisp.jfr

import jdk.jfr.{Description, Event, Label}
import org.wisp.ActorMessage

@Label("Undeliverable message")
@Description("Undeliverable message")
class UndeliverableMessage extends Event{

  @Label("actor message")
  var message:String = ""

}
