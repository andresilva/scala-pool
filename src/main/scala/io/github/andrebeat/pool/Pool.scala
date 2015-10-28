package io.github.andrebeat.pool

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.Duration

/**
  * A lease on an object requested from a `Pool` allowing the object to be accessed and then released
  * back to the pool when no longer needed.
  */
trait Lease[A <: AnyRef] {
  private[this] val released = new AtomicBoolean(false)
  protected def a: A
  protected def handleRelease(): Unit

  /**
    * Returns the object being leased by the pool. Throws an `IllegalStateException` if the lease
    * has already been released.
    * @return the object being leased by the pool.
    */
  def get(): A =
    if (!released.get()) a
    else throw new IllegalStateException("Tried to get an object from an already released lease.")

  /**
    * Releases the object back to the pool for reuse. When releasing an object it is mandatory that
    * there are no references to the returned object.
    */
  def release(): Unit = if (released.compareAndSet(false, true)) handleRelease
}

/**
  * A pool of objects that may be leased. It is expected that all implementations of this trait are
  * thread-safe.
  *
  * @tparam A the type of object to pool
  */
trait Pool[A <: AnyRef] {
  /**
    * Factory method for creating new objects.
    * @return a new object.
    */
  protected def factory(): A

  /**
    * Object "destructor". This method is called whenever the object is evicted from the pool.  For
    * example, when doing connection pooling it is necessary to close the connection whenever it is
    * evicted (i.e. permanently removed) from the pool.
    */
  protected def dispose(a: A): Unit

  /**
    * Try to acquire a lease for an object without blocking.
    * @return a lease for an object from this pool if available, `None` otherwise.
    */
  def tryAcquire(): Option[Lease[A]]

  /**
    * Try to acquire a lease for an object blocking at most until the given duration.
    * @param atMost maximum wait time for the lease to be available.
    * @return a lease for an object from this pool if available until the given duration, `None` otherwise.
    */
  def tryAcquire(atMost: Duration): Option[Lease[A]]

  /**
    * Acquire a lease for an object blocking if none is available.
    * @return a lease for an object from this pool.
    */
  def acquire(): Lease[A]

  /**
    * Drains the object pool, i.e. evicts every object currently pooled.
    */
  def drain(): Unit

  /**
    * Fills the object pool by creating (and pooling) new objects until the number of live objects
    * reaches the pool capacity.
    */
  def fill(): Unit

  /**
    * Returns the number of objects in the pool.
    * @return the number of objects in the pool.
    */
  def size(): Int

  /**
    * Returns the capacity of the pool, i.e. the maximum number of objects the pool can hold.
    * @return the capacity of the pool.
    */
  def capacity(): Int

  /**
    * Returns the number of live objects, i.e. the number of currently pooled objects plus leased
    * objects.
    * @return the number of live objects.
    */
  def live(): Int

  /**
    * Returns the number of leased objects.
    * @return the number of leased objects.
    */
  def leased(): Int = live - size
}
