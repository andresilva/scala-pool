package io.github.andrebeat.pool

import java.util.concurrent.atomic.AtomicLong

private[pool] class Jdk8Adder extends Adder {
  def increment() = ???
  def count() = ???
}

private[pool] class Jdk7Adder extends AtomicLong with Adder {
  def increment() = this.incrementAndGet
  def count() = this.get
}
