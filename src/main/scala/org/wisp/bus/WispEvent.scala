package org.wisp.bus

import jdk.jfr.{Description, Event, Label}
import org.wisp.ActorMessage

@Label("Wisp event")
@Description("Wisp event")
class WispEvent extends Event{

  @Label("actor message")
  var message:String = ""

}
