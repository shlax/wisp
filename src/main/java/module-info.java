module org.qwActor {
    requires org.scala.lang.scala3.library;
    requires scala.library;
    requires org.slf4j;
    requires jdk.jfr;
    exports org.qwActor;
    exports org.qwActor.jfr;
    exports org.qwActor.remote;
    exports org.qwActor.remote.client;
    exports org.qwActor.remote.cluster;
    exports org.qwActor.remote.codec;
    exports org.qwActor.stream;
    exports org.qwActor.stream.iterator;
    exports org.qwActor.stream.iterator.messages;
}