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
    referenceType: ReferenceType,
    _factory: () => A,
    _reset: A => Unit,
    _dispose: A => Unit
) extends ArrayBlockingQueuePool[A](capacity, referenceType) {

  implicit private[this] def function2TimerTask[A](f: () => A) = new TimerTask() { def run() = f() }

  final protected class ExpiringItem(r: Ref[A], val timerTask: TimerTask = () => {}) extends Item(r) {
    def consume() = timerTask.cancel()

    def offerSuccess() = timer.schedule(timerTask, maxIdleTime.toMillis)

    override def equals(that: Any) = that match {
      case that: ExpiringItem @unchecked => this.r eq that.r
      case _ => false
    }
  }

  private[this] val timer = new Timer(s"scala-pool-${ExpiringPool.count.getAndIncrement}", true)

  @inline protected[this] def factory() = _factory()
  @inline protected[this] def dispose(a: A) = _dispose(a)
  @inline protected[this] def reset(a: A) = _reset(a)

  @inline protected[this] def newItem(a: A) = {
    val r = Ref(a, referenceType)
    new ExpiringItem(r, () => if (items.remove(new ExpiringItem(r))) destroy(a))
  }
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
    referenceType: ReferenceType = Strong,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }
  ) =
    new ExpiringPool(capacity, maxIdleTime, referenceType, factory, reset, dispose)
}
