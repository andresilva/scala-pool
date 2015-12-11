package io.github.andrebeat.pool

import scala.annotation.tailrec

/**
  * A simple object pool that creates the objects as needed until a maximum number of objects has
  * been created.
  */
class SimplePool[A <: AnyRef](
    capacity: Int,
    referenceType: ReferenceType,
    _factory: () => A,
    _reset: A => Unit,
    _dispose: A => Unit,
    _healthCheck: A => Boolean
) extends ArrayBlockingQueuePool[A](capacity, referenceType) {

  @inline protected[this] def factory() = _factory()
  @inline protected[this] def dispose(a: A) = _dispose(a)
  @inline protected[this] def reset(a: A) = _reset(a)
  @inline protected[this] def healthCheck(a: A) = _healthCheck(a)

  final protected class SimpleItem(a: Ref[A]) extends Item(a) {
    def consume() = {}
    def offerSuccess() = {}
  }

  @inline protected[this] def newItem(a: A): Item = new SimpleItem(Ref(a, referenceType))
}

/**
  * Object containing factory methods for [[io.github.andrebeat.pool.SimplePool]].
  */
object SimplePool {
  def apply[A <: AnyRef](
    capacity: Int,
    referenceType: ReferenceType,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () },
    healthCheck: A => Boolean = { _: A => true }
  ) =
    new SimplePool(capacity, referenceType, factory, reset, dispose, healthCheck)
}
