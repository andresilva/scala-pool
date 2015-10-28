package io.github.andrebeat.pool

import java.util.{Timer, TimerTask}
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, NANOSECONDS}
import scala.language.implicitConversions

// TODO: Generalize SimplePool and ExpiringPool implementation:
// - Lease constructor must be abstract
// - The item stored in the pool must be abstract (in the expiring pool we want store the object and
//   its associated timer for cleanup):
//   - Fetching from the pool is abstract (B -> A)
//   - Adding to the pool is abstract (A -> B)

/**
  * An object pool that creates the objects as needed until a maximum number of objects has been
  * created and automatically evicts objects after they have been idle for a given amount of time.
  */
class ExpiringPool[A <: AnyRef](val capacity: Int, _factory: () => A, _reset: A => Unit, _dispose: A => Unit)
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

  private class ExpiringLease(protected val a: A) extends Lease[A] {
    protected def handleRelease() = {
      reset(a)
      tryOffer(a)
    }

    protected def handleInvalidate() = {
      destroy(a)
    }
  }

  @inline private def createOr(a: => A): A = {
    live.getAndIncrement match {
      case n if n < capacity =>
        factory()
      case _ =>
        decrementLive; a
    }
  }

  def acquire(): Lease[A] =
    new ExpiringLease(createOr(items.take()))

  def tryAcquire(): Option[Lease[A]] =
    Option(createOr(items.poll())).map(new ExpiringLease(_))

  def tryAcquire(atMost: Duration): Option[Lease[A]] =
    Option(createOr(items.poll(atMost.toNanos, NANOSECONDS))).map(new ExpiringLease(_))

  @tailrec final def drain() = {
    val i = Option(items.poll())
    if (i.nonEmpty) {
      destroy(i.get)
      drain()
    }
  }

  @tailrec final def fill() = {
    val io = Option(createOr(null.asInstanceOf[A]))
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
  * Object containing factory methods for `ExpiringPool`.
  */
object ExpiringPool {
  def apply[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }) =
    new ExpiringPool(capacity, factory, reset, dispose)
}
