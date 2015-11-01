package io.github.andrebeat.pool

import scala.concurrent.duration._

class ExpiringPoolSpec extends PoolSpec[ExpiringPool] {
  def Pool[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    referenceType: ReferenceType = Strong,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () }
  ) = ExpiringPool(capacity, 42.hours, factory, referenceType, reset, dispose)

  "A ExpiringPool" should {
    "evict idle objects" >> {
      val p = ExpiringPool(3, 50.millis, () => new Object)
      p.fill()

      p.size() === 3
      p.live() === 3

      Thread.sleep(200)

      p.size() === 0
      p.live() === 0
    }

    "when released, objects are counted as idle" >> {
      val p = ExpiringPool(3, 50.millis, () => new Object)
      p.fill()

      val l = p.acquire()

      p.size() === 2
      p.live() === 3

      Thread.sleep(200)

      p.size() === 0
      p.live() === 1

      l.release()

      p.size() === 1
      p.live() === 1

      Thread.sleep(200)

      p.size() === 0
      p.live() === 0
    }
  }
}
