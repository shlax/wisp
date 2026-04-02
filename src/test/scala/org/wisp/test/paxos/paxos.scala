package org.wisp.test.paxos

import org.wisp.remote.{RemoteLink, RemoteMessage}
import org.wisp.serializer.{ReadWrite, given}
import org.wisp.{AbstractActor, ActorLink, ActorScheduler}

import java.util.concurrent.CompletableFuture
import scala.concurrent.ExecutionContext
import scala.util.Random

type Value = String
type NodeId = Int

trait PaxosMessage extends RemoteMessage[String]

case class GenerationNumber(seq: Int, nodeId: NodeId) extends Ordered[GenerationNumber] derives ReadWrite {
  override def compare(o: GenerationNumber): Int = {
    var r = seq.compareTo(o.seq)
    if (r == 0) r = nodeId.compareTo(o.nodeId)
    r
  }
}

case class GenerationValue(n: GenerationNumber, value: Value) extends Ordered[GenerationValue] derives ReadWrite {
  override def compare(that: GenerationValue): Int = n.compare(that.n)
}

case class Prepare(from:NodeId, n: GenerationNumber) extends PaxosMessage derives ReadWrite {
  override def path: String = "acceptor"
}
case class Promise(from:NodeId, n: GenerationNumber, lastAccepted: Option[GenerationValue]) extends PaxosMessage derives ReadWrite {
  override def path: String = "proposer"
}
case class Accept(from:NodeId, n: GenerationValue) extends PaxosMessage derives ReadWrite {
  override def path: String = "acceptor"
}
case class Accepted(from:NodeId, n: GenerationValue) extends PaxosMessage derives ReadWrite {
  override def path: String = "proposer"
}

case class Ignore(n: GenerationNumber) extends PaxosMessage derives ReadWrite {
  override def path: String = "proposer"
}

case class TryRun(n: Option[GenerationNumber])

class Proposer(nodeId: NodeId, value:Value, acceptors: List[ActorLink], learner: CompletableFuture[String], scheduler: ActorScheduler)(using ExecutionContext) extends AbstractActor(scheduler) {
  val quorum: Int = {
    val s = acceptors.size
    val n2 = if (s % 2 == 0) s / 2 else (s - 1) / 2
    n2 + 1
  }

  var seq = 0

  var current : Option[GenerationValue] = None

  var promiseCount:Int = 0
  var acceptedCount:Int = 0

  override def accept(? : ActorLink): PartialFunction[Any, Unit] = {
    case TryRun(n) =>
      if(n.nonEmpty){
        if(n.get.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId "+n.get.nodeId+"/"+nodeId)
      }

      if(n.isEmpty || n.get.seq == seq) { // check if is for current run
        promiseCount = 0
        acceptedCount = 0

        seq += 1

        val genId = Prepare(nodeId,GenerationNumber(seq, nodeId))
        for (a <- Random.shuffle(acceptors)){
          Thread.sleep(Random.between(0, 100))
          a << genId
        }
      }

    case Promise(from, n, lastValue) =>
      println("Proposer["+nodeId+"]<|"+from+":Promise("+n+","+lastValue+")|>"+current+"|"+seq+"|"+acceptedCount+"|"+promiseCount)
      Thread.sleep(Random.between(0, 100))

      if(n.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId "+n.nodeId+"/"+nodeId)

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
          val av = Accept(nodeId, GenerationValue(n, current.map(_.value).getOrElse(value)))
          for (a <- Random.shuffle(acceptors)){
            Thread.sleep(Random.between(0, 100))
            a << av
          }
        }
      }

    case Accepted(from, gv) =>
      println("Proposer["+nodeId+"]<|"+from+":Accepted("+gv+")|>"+current+"|"+seq+"|"+acceptedCount+"|"+promiseCount)
      Thread.sleep(Random.between(0, 100))

      if(gv.n.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId "+gv.n.nodeId+"/"+nodeId)

      if(gv.n.seq == seq) { // check if is for current run
        acceptedCount += 1

        if (acceptedCount >= quorum) {
          val av = gv.value
          println(">>Proposer["+nodeId+"]<|"+from+":Accepted("+gv+")|>"+current+"|"+seq+"|"+acceptedCount+"|"+promiseCount)

          val lv = learner.getNow(av)
          if (av != lv) {
            throw new IllegalStateException("Proposer[" + nodeId + "] : " + lv + " != " + av + " " + gv)
          }
          learner.complete(av)
        }
      }

    case Ignore(n) =>
      if(n.nodeId != nodeId) throw new IllegalStateException("message:nodeId != nodeId "+n.nodeId+"/"+nodeId)

      if(n.seq == seq) { // check if is for current run
        this << TryRun(Some(n))
      }

  }

}

class Acceptor(nodeId:NodeId, link: NodeId => RemoteLink[PaxosMessage], scheduler: ActorScheduler) extends AbstractActor(scheduler) {

  var promised:Option[GenerationNumber] = None
  var lastValue:Option[GenerationValue] = None

  override def accept(? : ActorLink): PartialFunction[Any, Unit] = {
    case Prepare(from, n) =>
      println("Acceptor["+nodeId+"]<|"+from+":Prepare("+n+")|>"+promised+"|"+lastValue)
      Thread.sleep(Random.between(0, 100))

      if(promised.isEmpty){
        promised = Some(n)
        link(from) << Promise(nodeId, n, lastValue)
      }else{
        val act = promised.get
        if(n < act){ // ignore
          link(from) << Ignore(n)
        }else{
          promised = Some(n)
          link(from) << Promise(nodeId,n, lastValue)
        }
      }

    case Accept(from, gv) =>
      println("Acceptor["+nodeId+"]<|"+from+":Accept("+gv+")|>"+promised+"|"+lastValue)
      Thread.sleep(Random.between(0, 100))

      if(promised.isEmpty){
        promised = Some(gv.n)
        lastValue = Some(gv)
        link(from) << Accepted(nodeId, gv)
      }else {
        val act = promised.get
        if (gv.n < act) { // ignore
          link(from) << Ignore(gv.n)
        } else {
          promised = Some(gv.n)
          lastValue = Some(gv)
          link(from) << Accepted(nodeId, gv)
        }
      }

  }

}
