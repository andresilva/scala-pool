package io.github.andrebeat.pool

class SimplePoolSpec extends PoolSpec[SimplePool] {
  def Pool[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }
  ) = SimplePool(capacity, factory, Strong, reset, dispose)
}
