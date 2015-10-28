package io.github.andrebeat.pool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
  * A lease on an object requested from a `Pool` allowing the object to be accessed and then released
  * back to the pool when no longer needed.
  */
trait Lease[A] {
  private[this] val released = new AtomicBoolean(false)
  protected def a: A
  protected def handleRelease(): Unit

  /**
    * Returns the object being leased by the pool. No references to the returned object should ever
    * be kept. Throws an `IllegalStateException` if the lease has already been released.
    * @return the object being leased by the pool.
    */
  def get(): A =
    if (!released.get()) a
    else throw new IllegalStateException("Tried to get an object from an already released lease.")

  /**
    * Releases the object back to the pool for reuse.
    */
  def release(): Unit = if (released.compareAndSet(false, true)) handleRelease
}

/**
  * A pool of objects that may be leased. It is expected that all implementations of this trait are
  * thread-safe.
  *
  * @tparam A the type of object to pool
  */
trait Pool[A] {
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
  protected def dispose(a: A): Unit = {}

  /**
    * Try to acquire a lease for an object without blocking.
    * @return a lease for an object from this pool if available, `None` otherwise.
    */
  def tryAcquire(): Option[Lease[A]]

  /**
    * Acquire a lease for an object blocking if none is available.
    * @return a lease for an object from this pool.
    */
  def acquire(): Lease[A]
}

/**
  * A simple object pool that creates the objects as needed until a maximum number of objects has
  * been created.
  */
class SimplePool[A](maxSize: Int, _factory: () => A, _dispose: A => Unit) extends Pool[A] {
  private[this] val items = new ArrayBlockingQueue[A](maxSize)
  private[this] var created = 0

  protected def factory() = _factory()
  override protected def dispose(a: A) = _dispose(a)

  class SimpleLease(protected val a: A) extends Lease[A] {
    protected def handleRelease() = if (!items.offer(a)) dispose(a)
  }

  def acquire() = synchronized {
    if (items.size == 0 && created < maxSize) {
      created += 1
      new SimpleLease(factory())
    } else new SimpleLease(items.take())
  }

  def tryAcquire() = Option(items.poll()).map(new SimpleLease(_))
}

/**
  * Object containing factory methods for `SimplePool`.
  */
object SimplePool {
  def apply[A](maxSize: Int, factory: () => A, dispose: A => Unit = { _: A => () }) =
    new SimplePool(maxSize, factory, dispose)
}
