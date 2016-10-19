package io.github.andrebeat.pool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.{ Duration, NANOSECONDS }

/**
  * A generic object pooling implementation based on [[java.util.concurrent.ArrayBlockingQueue]].
  * This implementation relies on the thread-safety and blocking/non-blocking mechanisms of the
  * underlying data structure to implement the pool interface. Furthermore, for synchronization and
  * tracking of live instances an [[java.util.concurrent.atomic.AtomicInteger]] is used. No locks
  * are used in this implementation.
  *
  * The type of items inserted in the queue must implement the `Item` interface. This class defines
  * methods for consuming the item (e.g. disposing of any resources associated with it) and a method
  * that's called whenever an item is successfully inserted into the queue (useful for triggering a
  * side-effect). This class is also responsible for dealing with the reference type that's wrapping
  * the value (i.e. ensure calling its destructor if the value is defined).
  */
abstract class ArrayBlockingQueuePool[A <: AnyRef](
    val capacity: Int,
    val referenceType: ReferenceType) extends Pool[A] { pool =>
  abstract protected class Item(val r: Ref[A]) {
    def isDefined(): Boolean = {
      val ro = r.toOption
      ro.isDefined && healthCheck(ro.get)
    }

    /**
      * This method should only be called from this class and it is guaranteed that the value is
      * always defined before calling. Whenever this method is called it is considered that the
      * value is consumed.
      */
    def get(): A = {
      val a = r.toOption().get
      consume()
      a
    }

    /**
      * This method is only called whenever using a Soft/Weak reference that was invalidated by the
      * garbage collector. Whenever this method is called it is considered that the value is
      * consumed.
      */
    def destroy(): Unit = {
      r.toOption.map(pool.dispose)
      decrementLive
      consume()
    }

    /**
      * This method is called whenever the item is successfully inserted in the queue.
      */
    def offerSuccess(): Unit

    /**
      * This method is called whenever the item is consumed from the queue.
      */
    def consume(): Unit
  }

  protected[this] val items = new ArrayBlockingQueue[Item](capacity)
  private[this] val live = new AtomicInteger(0)

  @inline private[this] def decrementLive = live.getAndDecrement

  @inline protected[this] def destroy(a: A): Unit = {
    dispose(a)
    decrementLive
  }

  protected[this] def newItem(a: A): Item

  @inline private[this] def tryOffer(a: A) = {
    val item = newItem(a)
    if (items.offer(item)) item.offerSuccess()
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

  @inline private[this] def tryCreate(): Option[A] = {
    live.getAndIncrement match {
      case n if n < capacity =>
        Some(factory())
      case _ =>
        decrementLive
        None
    }
  }

  @inline private[this] def unwrapItem(io: => Option[Item], retry: Boolean = true): Option[A] =
    io match {
      case Some(i) if i.isDefined => Some(i.get)
      case Some(i) =>
        i.destroy()

        if (retry) unwrapItem(io)
        else None
      case _ => None
    }

  protected[this] def handleAcquire(): Lease[A] = {
    val item = unwrapItem(Option(items.poll()))

    if (item.isDefined) new PoolLease(item.get)
    else {
      tryCreate() match {
        case Some(i) => new PoolLease(i)
        case None => new PoolLease(unwrapItem(Some(items.take())).get)
      }
    }
  }

  protected[this] def handleTryAcquire(): Option[Lease[A]] = {
    val item = unwrapItem(Option(items.poll()))

    if (item.isDefined) Some(new PoolLease(item.get))
    else tryCreate().map(new PoolLease(_))
  }

  protected[this] def handleTryAcquire(atMost: Duration): Option[Lease[A]] = {
    val item = unwrapItem(Option(items.poll()))

    if (item.isDefined) Some(new PoolLease(item.get))
    else {
      tryCreate() match {
        case Some(i) => Some(new PoolLease(i))
        case None =>
          unwrapItem(Option(items.poll(atMost.toNanos, NANOSECONDS)), retry = false).map(new PoolLease(_))
      }
    }
  }

  @tailrec protected[this] final def handleDrain() = {
    val i = Option(items.poll())
    if (i.nonEmpty) {
      i.get.destroy()
      handleDrain()
    }
  }

  @tailrec protected[this] final def handleFill() = {
    val ao = tryCreate()
    if (ao.nonEmpty) {
      val a = ao.get
      reset(a)
      tryOffer(a)
      handleFill()
    }
  }

  def size() = items.size

  def live() = live.get()
}
