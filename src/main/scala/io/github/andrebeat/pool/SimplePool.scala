package io.github.andrebeat.pool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
  * A simple object pool that creates the objects as needed until a maximum number of objects has
  * been created.
  */
class SimplePool[A](maxSize: Int, _factory: () => A, _dispose: A => Unit) extends Pool[A] {
  private[this] val items = new ArrayBlockingQueue[A](maxSize)
  private[this] val alive = new AtomicInteger(0)

  protected def factory() = _factory()
  protected def dispose(a: A) = _dispose(a)

  private class SimpleLease(protected val a: A) extends Lease[A] {
    protected def handleRelease() = if (!items.offer(a)) dispose(a)
  }

  private def createOr(a: => A): A = {
    alive.getAndIncrement match {
      case n if n < maxSize =>
        factory()
      case _ =>
        alive.getAndDecrement
        a
    }
  }

  def acquire(): Lease[A] =
    new SimpleLease(createOr(items.take()))

  def tryAcquire(): Option[Lease[A]] =
    Option(createOr(items.poll())).map(new SimpleLease(_))
}

/**
  * Object containing factory methods for `SimplePool`.
  */
object SimplePool {
  def apply[A](maxSize: Int, factory: () => A, dispose: A => Unit = { _: A => () }) =
    new SimplePool(maxSize, factory, dispose)
}
