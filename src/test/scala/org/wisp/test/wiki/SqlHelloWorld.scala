package org.wisp.test.wiki

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.typed.StreamGraph
import org.wisp.stream.{Sink, Source}
import org.wisp.test.impl.tests.*
import org.wisp.closeable.*

import java.sql.{DriverManager, PreparedStatement, ResultSet}
import java.util.concurrent.atomic.AtomicInteger

class SqlHelloWorld {

  extension (i: ResultSet) {
    def asSource(thread:Thread, cnt: AtomicInteger): Source[ResultSet] = { () =>
      Assertions.assertEquals(thread, Thread.currentThread())
      cnt.incrementAndGet()
      if (i.next()) Some(i) else None
    }
  }

  extension (i: PreparedStatement) {
    def asSink[T](thread:Thread, cntAcc: AtomicInteger, cntCom: AtomicInteger)(fn: T => Unit): Sink[T] = new Sink{
      override def accept(x: T): Unit = {
        Assertions.assertEquals(thread, Thread.currentThread())
        cntAcc.incrementAndGet()
        fn.apply(x)
        i.addBatch()
      }

      override def complete(): Unit = {
        Assertions.assertEquals(thread, Thread.currentThread())
        cntCom.incrementAndGet()
        i.executeBatch()
      }
    }
  }

  @Test
  def selectInsert(): Unit = {
    DriverManager.getConnection("jdbc:h2:mem:")|{ conn =>
      conn.prepareStatement("create table src(a INT, b INT)")|(_.executeUpdate())
      conn.prepareStatement("insert into src(a, b) values(?, ?)")|{ ps =>
        for (i <- 1 to 10) {
          ps.setInt(1, i); ps.setInt(2, i+1)
          ps.addBatch()
        }
        ps.executeBatch()
      }

      conn.prepareStatement("create table dst(a INT, b INT, c INT)")|(_.executeUpdate())

      val thread = Thread.currentThread()
      val cntRs = new AtomicInteger
      val cntIns = new AtomicInteger
      val cntCom = new AtomicInteger

      // demo calculation
      using{ use =>
        val ins = use( conn.prepareStatement("insert into dst(a, b, c) values(?, ?, ?)") )

        // convert PreparedStatement to Sink[(Int, Int, Int)]
        val insert = ins.asSink[(Int, Int, Int)](thread, cntIns, cntCom){ x =>
          ins.setInt(1, x._1); ins.setInt(2, x._2); ins.setInt(3, x._3)
        }

        val sel = use( conn.prepareStatement("select a, b from src") )
        val rs = use( sel.executeQuery() )

        // convert ResultSet to Steam[(Int, Int)]
        val data = rs.asSource(thread, cntRs).map{ r =>
          ( r.getInt(1), r.getInt(2) )
        }

        ActorSystem() || { sys =>
          val graph = StreamGraph(sys)
          val r = graph.runnable(data, insert){ src =>
            // create 3 workers for calculation
            val workers = for(_ <- 1 to 3) yield src.map{ i => (i._1, i._2, i._1 + i._2) }
            // combine results from workers to single stream
            graph.zip(workers)
          }
          // run calculation
          r.run()
        }

      }

      Assertions.assertEquals(cntRs.get(), 11)
      Assertions.assertEquals(cntIns.get(), 10)
      Assertions.assertEquals(cntCom.get(), 1)

      // check result
      using { use =>
        val sel = use(conn.prepareStatement("select a, b, c from dst"))
        val rs = use(sel.executeQuery())

        cntRs.set(0)
        rs.asSource(thread, cntRs).forEach{ rs =>
          Assertions.assertEquals(rs.getInt(1) + rs.getInt(2), rs.getInt(3))
        }

        Assertions.assertEquals(cntRs.get(), 11)
      }

    }
  }

}
