package io.github.andrebeat

/**
  * This library provides classes for dealing with object pooling that allow:
  *   - blocking/non-blocking object acquisition
  *   - object invalidation
  *   - capping the number of pooled objects
  *   - creating new objects lazily, as needed
  *   - health checking
  *   - time-based pool eviction (idle instances)
  *   - GC-based pool eviction (soft and weak references)
  *   - efficient thread-safety
  *
  * ==Overview==
  * In order create a new [[io.github.andrebeat.pool.Pool]] the constructor method should be used
  * like so
  * {{{
  * scala> val pool = Pool(4, () => new Object)
  * pool: io.github.andrebeat.pool.SimplePool[Object] = _
  * scala> val lease = pool.acquire()
  * lease: io.github.andrebeat.pool.Lease[Object] = _
  * scala> lease.release()
  * }}}
  *
  * Additionally, in order to avoid manually releasing the lease after its used,
  * you can use the `use` method on the lease:
  *
  * {{{
  * scala> val pool = Pool(4, () => new Object)
  * pool: io.github.andrebeat.pool.SimplePool[Object] = _
  * scala> val lease = pool.acquire()
  * lease: io.github.andrebeat.pool.Lease[Object] = _
  * scala> lease(println) // the lease is released automatically after its used
  * java.lang.Object@7970d6d
  * }}}
  */
package object pool {}
