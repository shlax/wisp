package org.wisp.jfr;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;

@Category("Wisp")
@Label("Message created")
public class MessageCreated extends Event {

    /** message uuid, has the same value as {@link MessageProcessed#uuid} */
    @Label("UUID")
    public String uuid;

    /** message value {@link org.wisp.Message#value} */   
    @Label("Value")
    public String value;

}
