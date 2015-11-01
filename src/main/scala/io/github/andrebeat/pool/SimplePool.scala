package io.github.andrebeat.pool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.{ Duration, NANOSECONDS }

/**
  * A simple object pool that creates the objects as needed until a maximum number of objects has
  * been created.
  */
class SimplePool[A <: AnyRef](capacity: Int, _factory: () => A, _reset: A => Unit, _dispose: A => Unit)
    extends ArrayBlockingQueuePool[A](capacity) {
  type Item = A

  @inline protected[this] def factory() = _factory()
  @inline protected[this] def dispose(a: A) = _dispose(a)
  @inline protected[this] def reset(a: A) = _reset(a)

  @inline protected[this] def acquireItem(a: Item) = a
  @inline protected[this] def createItem(a: A) = a
  @inline protected[this] def offerSuccess(i: Item) = {}
}

/**
  * Object containing factory methods for `SimplePool`.
  */
object SimplePool {
  def apply[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }
  ) =
    new SimplePool(capacity, factory, reset, dispose)
}
