package io.github.andrebeat.pool

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.Duration

/**
  * A lease on an object requested from a `Pool` allowing the object to be accessed and then released
  * back to the pool when no longer needed.
  */
trait Lease[A <: AnyRef] {
  private[this] val dirty = new AtomicBoolean(false)
  protected[this] def a: A
  protected[this] def handleRelease(): Unit
  protected[this] def handleInvalidate(): Unit

  /**
    * Returns the object being leased by the pool. Throws an `IllegalStateException` if the lease
    * has already been released or invalidated.
    * @return the object being leased by the pool.
    */
  def get(): A =
    if (!dirty.get()) a
    else throw new IllegalStateException("Tried to get an object from an already released or invalidated lease.")

  /**
    * Releases the object back to the pool for reuse. When releasing an object it is mandatory that
    * there are no references to the returned object.
    *
    * If the lease has already been released or invalidated this method does nothing.
    */
  def release(): Unit = if (dirty.compareAndSet(false, true)) handleRelease

  /**
    * Invalidates the current lease. The object is "destroyed" and is no longer eligible to be
    * returned to the pool. Additionally, the number of live objects in the pool is decremented.
    *
    * If the lease has already been released or invalidated this method does nothing.
    */
  def invalidate(): Unit = if (dirty.compareAndSet(false, true)) handleInvalidate
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
    * Resets the internal state of object. This method is called on an object whenever it is
    * added/released back to the pool. For example, if pooling an object like a `ByteBuffer` it
    * might make sense to call its `reset()` method whenever the object is released to the pool, so
    * that its future users do not observe the internal state introduced by previous ones.
    */
  protected def reset(a: A): Unit

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
    * Returns the `ReferenceType` of the objects stored in the pool.
    */
  def referenceType: ReferenceType

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
    *
    * The value returned by this method is only accurate when the `referenceType` is `Strong`, since
    * GC-based eviction is checked only when trying to acquire an object.
    *
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
    *
    * The value returned by this method is only accurate when the `referenceType` is `Strong`, since
    * GC-based eviction is checked only when trying to acquire an object.
    *
    * @return the number of live objects.
    */
  def live(): Int

  /**
    * Returns the number of leased objects.
    *
    * The value returned by this method is only accurate when the `referenceType` is `Strong`, since
    * GC-based eviction is checked only when trying to acquire an object.
    *
    * @return the number of leased objects.
    */
  def leased(): Int = live - size
}