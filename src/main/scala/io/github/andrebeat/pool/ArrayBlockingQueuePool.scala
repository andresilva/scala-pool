package io.github.andrebeat.pool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.{ Duration, NANOSECONDS }

/**
  * A generic object pooling implementation based on `java.util.concurrent.ArrayBlockingQueue`.
  * This implementation relies on the thread-safety and blocking/non-blocking mechanisms of the
  * underlying data structure to implement the pool interface. Furthermore, for synchronization and
  * tracking of live instances an `AtomicInteger` is used. No locks are used in this implementation.
  *
  * The type of items inserted in the queue is generic and implementations of this class must
  * provide methods to create and acquire these items. Additionally, there's an "hook" that's called
  * whenever an item is succesfully inserted into the queue.
  */
abstract class ArrayBlockingQueuePool[A <: AnyRef](val capacity: Int) extends Pool[A] {
  type Item

  protected[this] val items = new ArrayBlockingQueue[Item](capacity)
  private[this] val live = new AtomicInteger(0)

  @inline private[this] def decrementLive = live.getAndDecrement

  @inline protected[this] def destroy(a: A): Unit = {
    dispose(a)
    decrementLive
  }

  protected[this] def acquireItem(i: Item): A
  protected[this] def createItem(a: A): Item
  protected[this] def offerSuccess(i: Item): Unit

  @inline private[this] def tryOffer(a: A) = {
    val item = createItem(a)
    if (items.offer(item)) offerSuccess(item)
    else destroy(a)
  }

  private class PoolLease(protected val a: A) extends Lease[A] {
    protected def handleRelease() = {
      reset(a)
      tryOffer(a)
    }

    protected def handleInvalidate() = {
      destroy(a)
    }
  }

  @inline private[this] def createOr(a: => Option[Item]): Option[A] = {
    live.getAndIncrement match {
      case n if n < capacity =>
        Some(factory())
      case _ =>
        decrementLive; a.map(acquireItem)
    }
  }

  def acquire(): Lease[A] =
    new PoolLease(createOr(Some(items.take())).get)

  def tryAcquire(): Option[Lease[A]] =
    createOr(Option(items.poll())).map(new PoolLease(_))

  def tryAcquire(atMost: Duration): Option[Lease[A]] =
    createOr(Option(items.poll(atMost.toNanos, NANOSECONDS))).map(new PoolLease(_))

  @tailrec final def drain() = {
    val i = Option(items.poll())
    if (i.nonEmpty) {
      destroy(acquireItem(i.get))
      drain()
    }
  }

  @tailrec final def fill() = {
    val ao = createOr(None)
    if (ao.nonEmpty) {
      val a = ao.get
      reset(a)
      tryOffer(a)
      fill()
    }
  }

  def size() = items.size

  def live() = live.get()
}
