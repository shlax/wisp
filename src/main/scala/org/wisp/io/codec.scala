package org.wisp.io

import org.wisp.closeable.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

object codec {

  def ioRead[T](in: ObjectInputStream)(using rw: ReadWrite[T]): T = {
    rw.read(in)
  }

  def fromBytes[T](in: Array[Byte])(using rw: ReadWrite[T]): T = {
    new ObjectInputStream(new ByteArrayInputStream(in))|{ oin =>
      ioRead(oin)
    }
  }

  extension [T](t: T)(using rw: ReadWrite[T]){
    def ioWrite(out: ObjectOutputStream): Unit = {
      rw.write(t, out)
    }
    
    def toBytes: Array[Byte] = {
      val bOut = new ByteArrayOutputStream()
      new ObjectOutputStream(bOut)|{ out =>
        ioWrite(out)
      }
      bOut.toByteArray
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
