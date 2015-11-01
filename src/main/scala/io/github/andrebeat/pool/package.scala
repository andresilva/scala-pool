package io.github.andrebeat

/**
  * This library provides classes for dealing with object pooling that allow:
  *   - blocking/non-blocking object acquisition
  *   - object invalidation
  *   - capping the number of pooled objects
  *   - creating new objects lazily, as needed
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
  * lease: io.github.andrebeat.pool.Lease[Array[Byte]] = _
  * scala> lease.release()
  * }}}
  */
package object pool {}
