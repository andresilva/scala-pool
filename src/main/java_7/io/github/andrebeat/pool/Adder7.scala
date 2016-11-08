package io.github.andrebeat.pool

import java.util.concurrent.atomic.AtomicLong

// $COVERAGE-OFF$
final private[pool] class Jdk8Adder extends Adder {
  def increment() = ???
  def count() = ???
}
// $COVERAGE-ON$

final private[pool] class Jdk7Adder extends AtomicLong with Adder {
  def increment() = this.incrementAndGet
  def count() = this.get
}
