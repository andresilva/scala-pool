package io.github.andrebeat.pool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.{ Duration, NANOSECONDS }

/**
  * A simple object pool that creates the objects as needed until a maximum number of objects has
  * been created.
  */
class SimplePool[A <: AnyRef](
    capacity: Int,
    referenceType: ReferenceType,
    _factory: () => A,
    _reset: A => Unit,
    _dispose: A => Unit
) extends ArrayBlockingQueuePool[A](capacity, referenceType) {

  @inline protected[this] def factory() = _factory()
  @inline protected[this] def dispose(a: A) = _dispose(a)
  @inline protected[this] def reset(a: A) = _reset(a)

  final protected class SimpleItem(a: Ref[A]) extends Item(a) {
    def consume() = {}
    def offerSuccess() = {}
  }

  @inline protected[this] def newItem(a: A): Item = new SimpleItem(Ref(a, referenceType))
}

/**
  * Object containing factory methods for `SimplePool`.
  */
object SimplePool {
  def apply[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    referenceType: ReferenceType = Strong,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }
  ) =
    new SimplePool(capacity, referenceType, factory, reset, dispose)
}
