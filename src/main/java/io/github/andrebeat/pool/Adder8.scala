package io.github.andrebeat.pool

import java.util.concurrent.atomic.{ LongAdder, AtomicLong }

final private[pool] class Jdk8Adder extends LongAdder with Adder {
  def count() = this.sum
}

// $COVERAGE-OFF$
final private[pool] class Jdk7Adder extends AtomicLong with Adder {
  def increment() = this.incrementAndGet
  def count() = this.get
}
// $COVERAGE-ON$
