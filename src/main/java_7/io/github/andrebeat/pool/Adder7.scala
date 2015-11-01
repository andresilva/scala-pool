package io.github.andrebeat.pool

import java.util.concurrent.atomic.AtomicLong

final private[pool] class Jdk8Adder extends Adder {
  def increment() = ???
  def count() = ???
}

final private[pool] class Jdk7Adder extends AtomicLong with Adder {
  def increment() = this.incrementAndGet
  def count() = this.get
}
