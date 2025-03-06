package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}

import scala.concurrent.ExecutionContext

class SplitStream(prev:ActorLink)(link: SplitStream#Split => Unit)(using executor: ExecutionContext) {

  class Split {
    protected[iterator] var to:List[SplitActorLink] = Nil

    def next : SplitActorLink = {
      val link = SplitActorLink()
      to = link :: to
      link
    }

  }

  class SplitActorLink extends ActorLink{
    override def accept(t: Message): Unit = ???
  }

  protected val next:List[SplitActorLink] = {
    val s = Split()
    link.apply(s)
    s.to.reverse
  }



}
