package org.qwActor.test.paxos

import org.qwActor.{Actor, ActorContext, ActorRef}

import java.util.concurrent.CompletableFuture

type Value = Any
type NodeId = Int

case class GenerationNumber(seq: Int, nodeId: NodeId) extends Ordered[GenerationNumber] {
  override def compare(o: GenerationNumber): Int = {
    var r = seq.compareTo(o.seq)
    if (r == 0) r = nodeId.compareTo(o.nodeId)
    r
  }
}

case class GenerationValue(n: GenerationNumber, value: Value) extends Ordered[GenerationValue] {
  override def compare(that: GenerationValue): Int = n.compare(that.n)
}

case class Prepare(n: GenerationNumber)
case class Promise(n: GenerationNumber, lastAccepted: Option[GenerationValue])
case class Accept(n: GenerationNumber, value: Value)
case class Accepted(n: GenerationNumber)

case class Ignore(n: GenerationNumber)

case class TryRun(n: Option[GenerationNumber])

class Proposer(nodeId: NodeId, value:Value, acceptors: List[ActorRef], learner: CompletableFuture[Any], context: ActorContext) extends Actor(context) {
  val quorum: Int = {
    val s = acceptors.size
    val n2 = if (s % 2 == 0) s / 2 else (s - 1) / 2
    n2 + 1
  }

  var seq = 0

  var current : Option[GenerationValue] = None

  var promiseCount:Int = 0
  var acceptedCount:Int = 0

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case TryRun(n) =>
      if(n.nonEmpty){
        if(n.get.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId")
      }

      if(n.isEmpty || n.get.seq == seq) { // check if is for current run
        promiseCount = 0
        acceptedCount = 0

        seq += 1

        val genId = Prepare(GenerationNumber(seq, nodeId))
        for (a <- acceptors) a.ask(genId).thenAccept(this)
      }

    case Promise(n, lastValue) =>
      if(n.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId")

      if(n.seq == seq) { // check if is for current run
        promiseCount += 1

        if (current.isEmpty) {
          current = lastValue
        } else if (lastValue.nonEmpty) {
          val currentVal: GenerationValue = current.get
          val lastNodeVal: GenerationValue = lastValue.get
          if (currentVal < lastNodeVal){
            current = Some(lastNodeVal)
          }
        }

        if (promiseCount == quorum) {
          val av = Accept(n, current.map(_.value).getOrElse(value))
          for (a <- acceptors) a.ask(av).thenAccept(this)
        }
      }

    case Accepted(n) =>
      if(n.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId")

      if(n.seq == seq) { // check if is for current run
        acceptedCount += 1

        if (acceptedCount >= quorum) {
          val av = current.map(_.value).getOrElse(value)

          val lv = learner.getNow(av)
          if (av != lv) {
            throw new IllegalStateException("" + System.nanoTime() + ": proposer-" + nodeId + " : " + lv + " != " + av + " " + n)
          }
          learner.complete(av)
        }
      }

    case Ignore(n) =>
      if(n.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId")

      if(n.seq == seq) { // check if is for current run
        this << TryRun(Some(n))
      }

  }

}


class Acceptor(context: ActorContext) extends Actor(context) {

  var promised:Option[GenerationNumber] = None
  var lastValue:Option[GenerationValue] = None

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case Prepare(n) =>

      if(promised.isEmpty){
        promised = Some(n)
        sender << Promise(n, lastValue)
      }else{
        val act = promised.get
        if(n < act){
          // ignore
          sender << Ignore(n)
        }else{
          promised = Some(n)
          sender << Promise(n, lastValue)
        }
      }

    case Accept(n, v) =>

      val act = promised.get
      if (n < act) {
        // ignore
        sender << Ignore(n)
      } else {
        lastValue = Some( GenerationValue(n, v) )
        sender << Accepted(n)
      }

  }

}

