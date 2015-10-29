package io.github.andrebeat.pool

import org.specs2.mutable.Specification
import scala.reflect.ClassTag

abstract class PoolSpec[P[_ <: AnyRef] <: Pool[_]](implicit ct: ClassTag[P[_]]) extends Specification {
  def Pool[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }): P[A]

  s"A ${ct.runtimeClass.getSimpleName}" should {
    "have a capacity" >> {
      val p = Pool(3, () => new Object)
      p.capacity() === 3
    }

    "return the number of pooled, live and leased objects" >> {
      val p = Pool(3, () => new Object)
      p.acquire()

      p.size() === 0
      p.leased() === 1
      p.live() === 1
    }

    "create objects lazily" >> {
      // TODO: use integer on factory to check that it is only set after acquiring
      val p = Pool(2, () => new Object)
      p.live() === 0
      p.size() === 0

      p.acquire()

      p.live() === 1
      p.size() === 0

      p.acquire()

      p.live() === 2
      p.size() === 0
    }

    "allow filling the pool" >> {
      // TODO: use integer in factory method to check that factory was called n times
      val p = Pool(3, () => new Object)
      p.live() === 0
      p.size() === 0

      p.fill()

      p.live() === 3
      p.size() === 3
    }

    "fill the pool taking into account live objects outside the pool" >> {
      val p = Pool(3, () => new Object)
      p.live() === 0
      p.size() === 0

      p.acquire()
      p.fill()

      p.live() === 3
      p.size() === 2
    }

    "allow draining the pool" >> {
      val p = Pool(3, () => new Object)
      p.fill()
      p.size() === 3

      p.drain()

      p.size() === 0
      p.live() === 0
    }

    "block when no objects available and all objects have been created" >> {
      todo
    }

    "only block until a given duration when trying to acquire an object" >> {
      todo
    }

    "call the reset method when adding/releasing an object to the pool" >> {
      "when filling" >> {
        todo
      }

      "when releasing" >> {
        todo
      }
    }

    "call the dispose method whenever an object is evicted from the pool" >> {
      "when invalidated" >> {
        todo
      }

      "when draining" >> {
        todo
      }

      "when added to an already full pool" >> {
        todo
      }
    }

    s"A ${ct.runtimeClass.getSimpleName} Lease" should {
      "allow being invalidated and removed from the pool" >> {
        todo
      }

      "allow being released back to the pool" >> {
        todo
      }

      "throw an exception when reading from an invalidated or released lease" >> {
        todo
      }
    }
  }
}
