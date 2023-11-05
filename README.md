implementation of the actor model for jvm with remoting/cluster

### Usage

#### [Hello World](https://github.com/shlax/wisp/tree/main/src/test/scala/org/wisp/test/tutorial/HelloWorld.scala)
```scala
class HelloActor(context: ActorContext) extends Actor(context) {
  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case a => println("hello " + a)
  }
}

Using(new ActorSystem) { system =>
  val actorRef = system.create(new HelloActor(_))
  actorRef << "world"
}.get 
```

#### [Ask Hello World](https://github.com/shlax/wisp/tree/main/src/test/scala/org/wisp/test/tutorial/AskHelloWorld.scala)
```scala
class HelloActor(context: ActorContext) extends Actor(context) {
  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case a => // actor reference has context set at it's creation 
      sender << "hello " + a
  }
}

Using(new ActorSystem) { system =>
  val actorRef = system.create(new HelloActor(_))
  val res = actorRef.ask("world").get()
  println(res.value)
}.get
```

#### [Stream Hello World](https://github.com/shlax/wisp/tree/main/src/test/scala/org/wisp/test/tutorial/StreamHelloWorld.scala)
```scala
case object GetResult

class SumActor(backpressure:WaitBarrier[Int], context: ActorContext) extends Actor(context) {
  backpressure.next(this)

  var sum = 0

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case i: Int =>
      backpressure.next(this)
      sum += i
    case GetResult =>
      sender << sum
  }
}

Using(new ActorSystem) { system =>
  val backpressure = WaitBarrier[Int]()
  val sumActor = system.create(c => new SumActor(backpressure, c))

  Flow(1 to 11){ f =>
    f.filter( n => n % 2 == 1).to(backpressure)
  }

  // messages from same thread will be processed in order
  println( sumActor.ask(GetResult).get().value )
}.get
```

#### [Actor Stream Hello World](https://github.com/shlax/wisp/tree/main/src/test/scala/org/wisp/test/tutorial/ActorStreamHelloWorld.scala)
```scala
Using(new ActorSystem) { system =>
    val range = (1 to 10).iterator
    
    val source = ActorSource[Int](range) // Iterator will be called from multiple threads
    val flow = system.create(c => ActorFlow[String](source, c)({
      case i : Int => ""+Thread.currentThread()+">"+i
    }))
    val sink = ActorSink(flow)(println)
    
    sink.start().get() // start processing data
}.get
```

#### [Blocking Actor Stream Hello World](https://github.com/shlax/wisp/tree/main/src/test/scala/org/wisp/test/tutorial/BlockingActorStreamHelloWorld.scala)
```scala
Using(new ActorSystem) { system =>
    val range = (1 to 10).iterator
    
    val source = ForEachSource[Int](range) // Iterator will be called from current thread
    val flow = system.create(c => ActorFlow[String](source, c)({
      case i : Int => "\t"+Thread.currentThread()+">"+i
    }))
    val sink = ActorSink(flow)(println)
    
    println("start: "+Thread.currentThread())
    
    val cf = sink.start() // start pulling data
    source.run() // iterate over range, will block current thread until all elements are not send
    cf.get() // wait for all messages to propagate
}.get
```

#### [Remote Client Hello World](https://github.com/shlax/wisp/tree/main/src/test/scala/org/wisp/test/tutorial/RemotingHelloWorld.scala)
```scala
class HelloActor(context: ActorContext) extends Actor(context) {
  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case a =>
      println("received " + a)
      sender << "hello "+a
  }
}

val system = RemoteSystem() // or ClusterSystem
system.create(new HelloActor(_)).bind("hello")
system.bind(new InetSocketAddress(4321))
// close RemoteSystem/ActorSystem 
system.shutdown().get(); system.close()

Using(new RemoteClient){ client =>
  // connect and wait(get())
  client.connect(new InetSocketAddress("127.0.0.1", 4321)).get()

  // reference to: system.create(new HelloActor(_)).bind("hello")
  val helloRef = client.get("hello")
  
  println(helloRef.ask("world").get().value)

  // wait for disconnect
  client.disconnect().get()
}.get
```

#### [Cluster Hello World](https://github.com/shlax/wisp/tree/main/src/test/scala/org/wisp/test/tutorial/ClusterHelloWorld.scala)
```scala
// create system1
val system1 = ClusterSystem()
system1.create(new HelloActor(_)).bind("hello") // HelloActor is from [Remote client Hello World]
system1.bind(new InetSocketAddress(4321))
// close RemoteSystem/ActorSystem 
system1.shutdown().get(); system1.close()


// create system2
val system2 = ClusterSystem()
system2.create(new HelloActor(_)).bind("hello")  // HelloActor is from [Remote client Hello World]
system2.bind(new InetSocketAddress(4322))
// close RemoteSystem/ActorSystem 
system2.shutdown().get(); system2.close()

// connect nodes 
system2.addNode(new InetSocketAddress("127.0.0.1", 4321)).get()

// loop over connected nodes 
system2.forEach{ (id, context) =>  // or system1.forEach{ (id, context) => 
  println("" + id + " " + context.get("hello").ask("world").get().value)
}
```
