package io.github.andrebeat.pool

/**
  * A trait to abstract over two distinct implementations of a long adder.
  *
  * The JDK8 introduces a new class `LongAdder` which when only using the `sum()` and `increment()`
  * methods is guaranteed to be sequentially consistent and a lot faster than an `AtomicLong`.
  * (http://concurrencyfreaks.blogspot.se/2013/09/longadder-is-not-sequentially-consistent.html).
  * On JDK7 an `AtomicLong` is used to implement this trait.
  *
  * The implementations of the `Adder`s are in different files to allow conditional compilation on
  * JDK7 and JDK8.
  */
private[pool] trait Adder {
  def increment(): Unit
  def count(): Long
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
