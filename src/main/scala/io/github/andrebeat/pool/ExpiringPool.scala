package io.github.andrebeat.pool

import java.util.{Timer, TimerTask}
import java.util.concurrent.ArrayBlockingQueue
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

// TODO: - use a duration for maxIdleTime?
//       - add tryAcquire with timeout parameter
/**
  * An object pool that creates the objects as needed until a maximum number of objects have been
  * created and automatically evicts objects after they have been idle for a given amount of time.
  */
class ExpiringPool[A <: AnyRef](maxSize: Int, maxIdleTime: Int, _factory: () => A, _dispose: A => Unit)
    extends Pool[A] {
  implicit private[this] def function2TimerTask[A](f: () => A) = new TimerTask() { def run() = f() }

  private class Item(val a: A, timerTask: TimerTask = () => {}) {
    def acquire() = {
      timerTask.cancel()
      a
    }

    override def equals(that: Any) = that match {
      case that: Item => this.a eq that.a
      case _ => false
    }
  }

  private[this] val lock = new Object
  private[this] val items = new ArrayBlockingQueue[Item](maxSize)
  private[this] val timer = new Timer(true)
  private[this] var created = 0

  protected def factory() = _factory()
  protected def dispose(a: A) = _dispose(a)

  class ExpiringLease(protected val a: A) extends Lease[A] {
    protected def handleRelease() = lock.synchronized {
      val timerTask: TimerTask = () => lock.synchronized { if (items.remove(new Item(a))) dispose(a) }
      if (items.offer(new Item(a, timerTask))) timer.schedule(timerTask, maxIdleTime)
      else dispose(a)
    }

    protected def handleInvalidate() = ???
  }

  def acquire() = lock.synchronized {
    if (items.size == 0 && created < maxSize) {
      created += 1
      new ExpiringLease(factory())
    } else {
      new ExpiringLease(items.take().acquire())
    }
  }

  def tryAcquire() = lock.synchronized {
    Option(items.poll()).map { item => new ExpiringLease(item.acquire()) }
  }

  def tryAcquire(atMost: Duration): Option[Lease[A]] = ???

  def drain() = ???

  def fill() = ???

  def size() = items.size

  def capacity() = ???

  def live() = ???
}

object ExpiringPool {
  def apply[A <: AnyRef](maxSize: Int, maxIdleTime: Int, factory: () => A, dispose: A => Unit = { _: A => () }) =
    new ExpiringPool(maxSize, maxIdleTime, factory, dispose)
}
