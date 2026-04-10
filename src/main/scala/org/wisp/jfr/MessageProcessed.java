package org.wisp.jfr;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import org.wisp.Actor;

@Category("Wisp")
@Label("Message processed")
public class MessageProcessed extends Event {

    @Label("Actor")
    public Class<? extends Actor> actor;
    
    /** message uuid, has the same value as {@link MessageCreated#uuid} */
    @Label("UUID")
    public String uuid;

    /** message value {@link org.wisp.Message#value} */
    @Label("Value")
    public String value;
    
}
