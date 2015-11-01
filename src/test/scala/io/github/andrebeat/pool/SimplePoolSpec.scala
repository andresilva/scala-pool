package io.github.andrebeat.pool

class SimplePoolSpec extends PoolSpec[SimplePool] {
  def Pool[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    referenceType: ReferenceType = ReferenceType.Strong,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }
  ) = SimplePool(capacity, factory, referenceType, reset, dispose)
}
