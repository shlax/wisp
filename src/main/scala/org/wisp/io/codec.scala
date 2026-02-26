package org.wisp.io

import java.io.{ObjectInputStream, ObjectOutputStream}

object codec {

  def read[T](in: ObjectInputStream)(using rw: ReadWrite[T]): T = {
    rw.read(in)
  }

  extension [T](t: T)(using rw: ReadWrite[T]){
    def write(out: ObjectOutputStream): Unit = {
      rw.write(t, out)
    }
  }

  given ReadWrite[Int] = new ReadWrite[Int] {
    override def read(in: ObjectInputStream): Int = {
      in.readInt()
    }
    override def write(t: Int, out: ObjectOutputStream): Unit = {
      out.writeInt(t)
    }
  }

  given ReadWrite[String] = new ReadWrite[String] {
    override def read(in: ObjectInputStream): String = {
      in.readUTF()
    }
    override def write(t: String, out: ObjectOutputStream): Unit = {
      out.writeUTF(t)
    }
  }

}
