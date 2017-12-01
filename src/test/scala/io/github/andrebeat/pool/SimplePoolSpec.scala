package io.github.andrebeat.pool

class SimplePoolSpec extends PoolSpec[SimplePool] {
  def pool[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    referenceType: ReferenceType = ReferenceType.Strong,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () },
    healthCheck: A => Boolean = { _: A => true }) =
    SimplePool(capacity, referenceType, factory, reset, dispose, healthCheck)
}
