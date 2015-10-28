package io.github.andrebeat.pool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, NANOSECONDS}

/**
  * A simple object pool that creates the objects as needed until a maximum number of objects has
  * been created.
  */
class SimplePool[A <: AnyRef](val capacity: Int, _factory: () => A, _reset: A => Unit, _dispose: A => Unit)
    extends Pool[A] {
  private[this] val items = new ArrayBlockingQueue[A](capacity)
  private[this] val live = new AtomicInteger(0)

  protected def factory() = _factory()
  protected def dispose(a: A) = _dispose(a)
  protected def reset(a: A) = _reset(a)

  private class SimpleLease(protected val a: A) extends Lease[A] {
    protected def handleRelease() = reset(a); if (!items.offer(a)) dispose(a)
    protected def handleInvalidate() = {
      dispose(a); live.getAndDecrement
    }
  }

  private def createOr(a: => A): A = {
    live.getAndIncrement match {
      case n if n < capacity =>
        factory()
      case _ =>
        live.getAndDecrement; a
    }
  }

  def acquire(): Lease[A] =
    new SimpleLease(createOr(items.take()))

  def tryAcquire(): Option[Lease[A]] =
    Option(createOr(items.poll())).map(new SimpleLease(_))

  def tryAcquire(atMost: Duration): Option[Lease[A]] =
    Option(createOr(items.poll(atMost.toNanos, NANOSECONDS))).map(new SimpleLease(_))

  @tailrec final def drain() = {
    val i = Option(items.poll())
    if (i.nonEmpty) {
      dispose(i.get); live.getAndDecrement
      drain()
    }
  }

  @tailrec final def fill() = {
    val i = Option(createOr(null.asInstanceOf[A]))
    if (i.nonEmpty) {
      reset(i.get)
      items.offer(i.get)
      fill()
    }
  }

  def size() = items.size

  def live() = live.get()
}

/**
  * Object containing factory methods for `SimplePool`.
  */
object SimplePool {
  def apply[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }) =
    new SimplePool(capacity, factory, reset, dispose)
}
