package io.github.andrebeat.pool

import java.util.{ Timer, TimerTask }
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import java.util.concurrent.atomic.AtomicInteger

/** An object pool that creates the objects as needed until a maximum number of objects has been created and
  * automatically evicts objects after they have been idle for a given amount of time.
  */
class ExpiringPool[A <: AnyRef](
    capacity: Int,
    referenceType: ReferenceType,
    val maxIdleTime: Duration,
    _factory: () => A,
    _reset: A => Unit,
    _dispose: A => Unit,
    _healthCheck: A => Boolean
) extends ArrayBlockingQueuePool[A](capacity, referenceType) {

  implicit private[this] def function2TimerTask[A](f: () => A): TimerTask = new TimerTask() { def run() = f() }

  final protected class ExpiringItem(val id: Long, r: Ref[A], val timerTask: TimerTask = () => {}) extends Item(r) {

    def consume() = timerTask.cancel()

    def offerSuccess() =
      try {
        timer.schedule(timerTask, maxIdleTime.toMillis)
      } catch {
        case e: IllegalStateException =>
        // it is possible that this item has already been consumed by the time this method executes.
        // this will trigger an exception since we're trying to schedule a task that's already been
        // canceled.
      }

    override def equals(that: Any) =
      that match {
        case that: ExpiringItem @unchecked => this.id == that.id
        case _ => false
      }
  }

  private[pool] val timer = new Timer(s"scala-pool-${ExpiringPool.count.getAndIncrement}", true)
  private[this] val adder = Adder()

  @inline protected[this] def factory() = _factory()
  @inline protected[this] def dispose(a: A) = _dispose(a)
  @inline protected[this] def reset(a: A) = _reset(a)
  @inline protected[this] def healthCheck(a: A) = _healthCheck(a)

  @inline protected[this] def handleClose() = {
    timer.cancel()
  }

  @inline protected[this] def newItem(a: A) = {
    adder.increment()
    val id = adder.count()
    val r = Ref(a, referenceType)
    new ExpiringItem(
      id,
      r,
      () => {
        val i = new ExpiringItem(id, r)
        if (items.remove(i)) i.destroy()
      }
    )
  }
}

/** Object containing factory methods for [[io.github.andrebeat.pool.ExpiringPool]].
  */
object ExpiringPool {
  private val count = new AtomicInteger(0)

  def apply[A <: AnyRef](
      capacity: Int,
      referenceType: ReferenceType,
      maxIdleTime: Duration,
      factory: () => A,
      reset: A => Unit = { _: A => () },
      dispose: A => Unit = { _: A => () },
      healthCheck: A => Boolean = { _: A => true }
  ) = new ExpiringPool(capacity, referenceType, maxIdleTime, factory, reset, dispose, healthCheck)
}
