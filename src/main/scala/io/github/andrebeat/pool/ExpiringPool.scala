package io.github.andrebeat.pool

import java.util.{ Timer, TimerTask }
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.tailrec
import scala.concurrent.duration.{ Duration, NANOSECONDS }

/**
  * An object pool that creates the objects as needed until a maximum number of objects has been
  * created and automatically evicts objects after they have been idle for a given amount of time.
  */
class ExpiringPool[A <: AnyRef](
    capacity: Int,
    val maxIdleTime: Duration,
    _factory: () => A,
    _reset: A => Unit,
    _dispose: A => Unit
) extends ArrayBlockingQueuePool[A](capacity) {
  type Item = ExpiringItem

  implicit private[this] def function2TimerTask[A](f: () => A) = new TimerTask() { def run() = f() }

  protected class ExpiringItem(val a: A, val timerTask: TimerTask = () => {}) {
    def acquire() = {
      timerTask.cancel()
      a
    }

    override def equals(that: Any) = that match {
      case that: Item => this.a eq that.a
      case _ => false
    }
  }

  private[this] val timer = new Timer(s"scala-pool-${ExpiringPool.count.getAndIncrement}", true)

  @inline protected[this] def factory() = _factory()
  @inline protected[this] def dispose(a: A) = _dispose(a)
  @inline protected[this] def reset(a: A) = _reset(a)

  @inline protected[this] def acquireItem(a: Item) = a.acquire()
  @inline protected[this] def createItem(a: A) = new Item(a, () => if (items.remove(new Item(a))) destroy(a))
  @inline protected[this] def offerSuccess(i: Item) = timer.schedule(i.timerTask, maxIdleTime.toMillis)
}

/**
  * Object containing factory methods for `ExpiringPool`.
  */
object ExpiringPool {
  private val count = new AtomicInteger(0)

  def apply[A <: AnyRef](
    capacity: Int,
    maxIdleTime: Duration,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }
  ) =
    new ExpiringPool(capacity, maxIdleTime, factory, reset, dispose)
}
