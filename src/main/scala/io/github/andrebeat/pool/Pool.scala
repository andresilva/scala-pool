package io.github.andrebeat.pool

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.Duration

/**
  * A lease on an object requested from a [[io.github.andrebeat.pool.Pool]] allowing the object to
  * be accessed and then released back to the pool when no longer needed.
  *
  * @tparam A the type of object stored in this lease
  */
trait Lease[A <: AnyRef] {
  private[this] val dirty = new AtomicBoolean(false)
  protected[this] def a: A
  protected[this] def handleRelease(): Unit
  protected[this] def handleInvalidate(): Unit

  /**
    * Returns the object being leased by the pool. Throws an [[java.lang.IllegalStateException]]
    * if the lease has already been released or invalidated.
    * @return the object being leased by the pool.
    */
  def get(): A =
    if (!dirty.get()) a
    else throw new IllegalStateException("Tried to get an object from an already released or invalidated lease.")

  /**
    * Releases the object back to the pool for reuse. When releasing an object it is mandatory that
    * there are no references to the returned object.
    *
    * When an object is released to a pool that has already been closed it is "destroyed".
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

  /**
    * Gets the value from the lease, passing it onto the provided function and
    * releasing it back to the pool after the function is run.
    *
    * If needed, it is possible to invalidate the lease from inside the provided
    * function, for example:
    *
    * {{{
    * val lease = pool.acquire()
    * val x = lease { v =>
    *   if (/* invalid condition */) { lease.invalidate(); None }
    *   else Some(/* result */)
    * }
    * }}}
    *
    * It is important that no references are kept to the leased object after
    * this method finishes. For example, the following code is invalid since the
    * variable `x` holds a reference to an object that was returned to the pool.
    *
    * {{{
    * val lease = pool.acquire()
    * val x = lease(identity)
    * }}}
    *
    * @tparam B the type of object returned by the function `f`
    * @param f a function that uses the value stored in the lease to produce a new value
    * @return the value produced by the function `f`.
    */
  def apply[B](f: A => B): B = try {
    f(get())
  } finally {
    release()
  }

  /**
    * Gets the value from the lease, passing it onto the provided function and
    * releasing it back to the pool after the function is run.
    *
    * Alias method for `apply`.
    *
    * @see [[io.github.andrebeat.pool.Lease.apply]]
    */
  def use[B](f: A => B): B = apply(f)
}

/**
  * A pool of objects that may be leased. It is expected that all implementations of this trait are
  * thread-safe.
  *
  * @tparam A the type of object to pool
  */
trait Pool[A <: AnyRef] {
  import Pool.ClosedPoolException

  protected[this] val closed = new AtomicBoolean(false)

  protected def handleTryAcquire(): Option[Lease[A]]
  protected def handleTryAcquire(atMost: Duration): Option[Lease[A]]
  protected def handleAcquire(): Lease[A]
  protected def handleDrain(): Unit
  protected def handleFill(): Unit
  protected def handleClose(): Unit

  /**
    * Factory method for creating new objects.
    * @return a new object.
    */
  protected def factory(): A

  /**
    * Resets the internal state of object. This method is called on an object whenever it is
    * added/released back to the pool. For example, if pooling an object like a
    * [[java.nio.ByteBuffer]] it might make sense to call its `reset()` method whenever the object
    * is released to the pool, so that its future users do not observe the internal state introduced
    * by previous ones.
    */
  protected def reset(a: A): Unit

  /**
    * Object "destructor". This method is called whenever the object is evicted from the pool.  For
    * example, when doing connection pooling it is necessary to close the connection whenever it is
    * evicted (i.e. permanently removed) from the pool.
    */
  protected def dispose(a: A): Unit

  /**
    * An health check that is performed on an object before its leased from the
    * pool. If the health check passes the object is successfully leased.
    * Otherwise, the object is destroyed (and a new one is fetched or created)
    */
  protected def healthCheck(a: A): Boolean

  /**
    * Try to acquire a lease for an object without blocking.
    * @return a lease for an object from this pool if available, `None` otherwise.
    * @throws ClosedPoolException If this pool is closed.
    */
  def tryAcquire(): Option[Lease[A]] =
    if (!closed.get()) handleTryAcquire else throw new ClosedPoolException

  /**
    * Try to acquire a lease for an object blocking at most until the given duration.
    * @param atMost maximum wait time for the lease to be available.
    * @return a lease for an object from this pool if available until the given duration, `None` otherwise.
    * @throws ClosedPoolException If this pool is closed.
    */
  def tryAcquire(atMost: Duration): Option[Lease[A]] =
    if (!closed.get()) handleTryAcquire(atMost) else throw new ClosedPoolException

  /**
    * Returns the [[io.github.andrebeat.pool.ReferenceType]] of the objects stored in the pool.
    */
  def referenceType: ReferenceType

  /**
    * Acquire a lease for an object blocking if none is available.
    * @return a lease for an object from this pool.
    * @throws ClosedPoolException If this pool is closed.
    */
  def acquire(): Lease[A] =
    if (!closed.get()) handleAcquire else throw new ClosedPoolException

  /**
    * Drains the object pool, i.e. evicts every object currently pooled.
    * @throws ClosedPoolException If this pool is closed.
    */
  def drain(): Unit =
    if (!closed.get()) handleDrain else throw new ClosedPoolException

  /**
    * Fills the object pool by creating (and pooling) new objects until the number of live objects
    * reaches the pool capacity.
    * @throws ClosedPoolException If this pool is closed.
    */
  def fill(): Unit =
    if (!closed.get()) handleFill else throw new ClosedPoolException

  /**
    * Returns the number of objects in the pool.
    *
    * The value returned by this method is only accurate when the `referenceType` is
    * [[io.github.andrebeat.pool.ReferenceType.Strong]], since GC-based eviction is checked only
    * when trying to acquire an object.
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
    * The value returned by this method is only accurate when the `referenceType` is
    * [[io.github.andrebeat.pool.ReferenceType.Strong]], since GC-based eviction is checked only
    * when trying to acquire an object.
    *
    * @return the number of live objects.
    */
  def live(): Int

  /**
    * Returns the number of leased objects.
    *
    * The value returned by this method is only accurate when the `referenceType` is
    * [[io.github.andrebeat.pool.ReferenceType.Strong]], since GC-based eviction is checked only
    * when trying to acquire an object.
    *
    * @return the number of leased objects.
    */
  def leased(): Int = live - size

  /**
    * Closes this pool, and properly disposes of each pooled object, releasing any resources
    * associated with the pool (e.g. background timer threads).
    *
    * If the pool has already been closed this method does nothing.
    */
  def close(): Unit = {
    if (closed.compareAndSet(false, true)) {
      handleDrain
      handleClose
    }
  }
}

/**
  * Object containing factory methods for [[io.github.andrebeat.pool.Pool]].
  */
object Pool {
  /**
    * An exception that is thrown when trying to use a closed pool.
    */
  class ClosedPoolException extends Exception

  /**
    * Creates a new [[io.github.andrebeat.pool.ExpiringPool]] or
    * [[io.github.andrebeat.pool.SimplePool]] instance depending on whether a non-zero and finite
    * `maxIdleTime` is set or not.
    *
    * @param capacity the maximum capacity of the pool
    * @param factory the function used to create new objects in the pool
    * @param referenceType the reference type of objects in the [[io.github.andrebeat.pool.Pool]].
    *                      [[io.github.andrebeat.pool.ReferenceType.Soft]] and
    *                      [[io.github.andrebeat.pool.ReferenceType.Weak]] reference are eligible
    *                      for collection by the GC
    * @param maxIdleTime the maximum amount of the time that objects are allowed to
    *        idle in the pool before being evicted
    * @param reset the function used to reset objects in the pool (called when leasing an object from the pool)
    * @param dispose the function used to destroy an object from the pool
    * @param healthCheck the predicate used to test whether an object is healthy and should be used,
    *                    or destroyed otherwise.
    * @return a new instance of [[io.github.andrebeat.pool.Pool]].
    */
  def apply[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    referenceType: ReferenceType = ReferenceType.Strong,
    maxIdleTime: Duration = Duration.Inf,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () },
    healthCheck: A => Boolean = { _: A => true }): Pool[A] =
    if (maxIdleTime.isFinite)
      ExpiringPool(capacity, referenceType, maxIdleTime, factory, reset, dispose, healthCheck)
    else
      SimplePool(capacity, referenceType, factory, reset, dispose, healthCheck)
}
