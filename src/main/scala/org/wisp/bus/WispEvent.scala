package org.wisp.bus

import jdk.jfr.{Description, Event, Label}

@Label("Wisp event")
@Description("Wisp event")
class WispEvent extends Event{

  @Label("message")
  var message:String = ""

}
