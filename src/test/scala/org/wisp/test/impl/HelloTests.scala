package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.using.*
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox}

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class HelloTests {

  @Test
  def helloWorld(): Unit = {
    val cd = new CountDownLatch(1)
    val ref = AtomicReference[Any]()

    class HelloActor(in: Inbox) extends Actor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a =>
          ref.set(a)
          cd.countDown()
      }
    }

    new ActorSystem()|{ sys =>
      val hello = sys.create(HelloActor(_))
      hello << "Hello world"

      cd.await()
    }

    Assertions.assertEquals("Hello world", ref.get())
  }

  @Test
  def helloAsk():Unit = {
    val cd = new CountDownLatch(1)
    val ref = AtomicReference[Any]()

    class HelloActor(in: Inbox) extends Actor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a => from << "Hello " + a
      }
    }

    ActorSystem() | ( _.as { sys =>

      val hello = sys.create(HelloActor(_))
      hello.ask("world").future.onComplete { e =>
        ref.set(e.get.value)
        cd.countDown()
      }

      cd.await()
    })

    Assertions.assertEquals("Hello world", ref.get())
  }

}
