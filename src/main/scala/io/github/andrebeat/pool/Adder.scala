package io.github.andrebeat.pool

import java.util.concurrent.atomic.{ AtomicLong, LongAdder }

/**
  * A trait to abstract over two distinct implementations of a long adder.
  *
  * The JDK8 introduces a new class `LongAdder` which when only using the `sum()` and `increment()`
  * methods is guaranteed to be sequentially consistent and a lot faster than an `AtomicLong`.
  * (http://concurrencyfreaks.blogspot.se/2013/09/longadder-is-not-sequentially-consistent.html).
  *
  *  On JDK7 an `AtomicLong` is used to implement this trait.
  */
private[pool] trait Adder {
  def increment(): Unit
  def count(): Long
}

private[pool] class Jdk7Adder extends AtomicLong with Adder {
  def increment() = this.incrementAndGet
  def count() = this.get
}

private[pool] class Jdk8Adder extends LongAdder with Adder {
  def count() = this.sum
}

private[pool] object Adder {
  def apply() =
    try {
      this.getClass.getClassLoader().loadClass("java.util.concurrent.atomic.LongAdder")
      new Jdk8Adder()
    } catch {
      case e: ClassNotFoundException =>
        new Jdk7Adder()
    }
}
