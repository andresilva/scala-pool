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

  @inline private[this] def decrementLive = live.getAndDecrement

  @inline protected def factory() = _factory()
  @inline protected def dispose(a: A) = _dispose(a)
  @inline protected def reset(a: A) = _reset(a)

  @inline private[this] def destroy(a: A) = {
    dispose(a)
    decrementLive
  }

  @inline private[this] def tryOffer(a: A) = if (!items.offer(a)) destroy(a)

  private class SimpleLease(protected val a: A) extends Lease[A] {
    protected def handleRelease() = {
      reset(a)
      tryOffer(a)
    }

    protected def handleInvalidate() = {
      destroy(a)
    }
  }

  @inline private def createOr(a: => Option[A]): Option[A] = {
    live.getAndIncrement match {
      case n if n < capacity =>
        Some(factory())
      case _ =>
        decrementLive; a
    }
  }

  def acquire(): Lease[A] =
    new SimpleLease(createOr(Some(items.take())).get)

  def tryAcquire(): Option[Lease[A]] =
    createOr(Option(items.poll())).map(new SimpleLease(_))

  def tryAcquire(atMost: Duration): Option[Lease[A]] =
    createOr(Option(items.poll(atMost.toNanos, NANOSECONDS))).map(new SimpleLease(_))

  @tailrec final def drain() = {
    val i = Option(items.poll())
    if (i.nonEmpty) {
      destroy(i.get)
      drain()
    }
  }

  @tailrec final def fill() = {
    val io = createOr(None)
    if (io.nonEmpty) {
      val i = io.get
      reset(i)
      tryOffer(i)
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
