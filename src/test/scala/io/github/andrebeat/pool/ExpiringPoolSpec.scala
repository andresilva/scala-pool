package io.github.andrebeat.pool

import scala.concurrent.duration._

class ExpiringPoolSpec extends PoolSpec[ExpiringPool] {
  def Pool[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }) = ExpiringPool(capacity, Duration.Inf, factory, reset, dispose)
}
