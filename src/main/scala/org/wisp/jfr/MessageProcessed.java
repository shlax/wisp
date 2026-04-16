package org.wisp.jfr;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Event that is created when a message is processed
 */
@Category("Wisp")
@Label("Message processed")
public class MessageProcessed extends Event {

    /**
     * class that processed the message
     */
    @Label("Consumer")
    public Class<?> consumer;
    
    /**
     * message uuid, has the same value as {@link MessageCreated#uuid}
     */
    @Label("UUID")
    public String uuid;

    /**
     * message value {@link org.wisp.Message#value}
     */
    @Label("Value")
    public String value;
    
}
