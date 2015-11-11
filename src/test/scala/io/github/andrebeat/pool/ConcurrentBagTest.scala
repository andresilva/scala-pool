package io.github.andrebeat.pool

import java.util.concurrent.LinkedTransferQueue
import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.util.Random

object ConcurrentBagTest extends App {
  val N_THREADS = 8
  val N_OPERATIONS = 1000000

  case class MyObject(val x: Long)

  val queue = new LinkedTransferQueue[MyObject]()
  val bag = new ConcurrentBag[MyObject]()

  val adder = Adder()

  val takeProb = 0
  val addProb = 1 - takeProb

  def work() = {
    if (Random.nextDouble < takeProb) {
      queue.poll()
      bag.tryTake()
    } else {
      adder.increment()
      val i = adder.count()

      val myObject = new MyObject(i)
      queue.add(myObject)
      bag.add(myObject)
    }
  }

  val threads =
    for (i <- 1 to N_THREADS) yield new Thread() {
      override def run() {
        var i = 0
        while (i < N_OPERATIONS) {
          work()
          i += 1
        }
      }
    }

  threads.foreach(_.start())
  threads.foreach(_.join())

  val queueSize = queue.size
  val bagSize = {
    var n = 0
    var i = bag.tryTake()
    while (i.nonEmpty) {
      n += 1
      i = bag.tryTake()
      if (i.isEmpty) bag.tryTake()
    }
    n
  }

  println(s"queueSize: $queueSize")
  println(s"bagSize: $bagSize")

  println(s"Pass? ${queueSize == bagSize}")
}
