package org.wisp

/**
 * Represents a message passed between actors
 *
 * @param sender the actor link of the message sender
 * @param value  the payload of the message
 */
case class Message(sender:ActorLink, value:Any)
