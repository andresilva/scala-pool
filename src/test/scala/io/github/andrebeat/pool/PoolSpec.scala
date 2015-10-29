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

    "create objects lazily" >> {
      val p = Pool(3, () => new Object)
      p.live() === 0
      p.acquire()
      p.live() === 1
    }
  }
}
