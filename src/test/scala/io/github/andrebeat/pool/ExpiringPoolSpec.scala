package io.github.andrebeat.pool

import scala.concurrent.duration._

class ExpiringPoolSpec extends PoolSpec[ExpiringPool] with TestHelper {
  def pool[A <: AnyRef](
    capacity: Int,
    factory: () => A,
    referenceType: ReferenceType = ReferenceType.Strong,
    reset: A => Unit = { _: A => () },
    dispose: A => Unit = { _: A => () },
    healthCheck: A => Boolean = { _: A => true }) =
    ExpiringPool(capacity, referenceType, 42.hours, factory, reset, dispose, healthCheck)

  "A ExpiringPool" should {
    "evict idle objects" >> {
      val p = ExpiringPool(3, ReferenceType.Strong, 50.millis, () => new Object)
      p.fill()

      p.size() === 3
      p.live() === 3

      sleep(200.millis)

      p.size() === 0
      p.live() === 0
    }

    "when released, objects are counted as idle" >> {
      val p = ExpiringPool(3, ReferenceType.Strong, 50.millis, () => new Object)
      p.fill()

      val l = p.acquire()

      p.size() === 2
      p.live() === 3

      sleep(200.millis)

      p.size() === 0
      p.live() === 1

      l.release()

      p.size() === 1
      p.live() === 1

      sleep(200.millis)

      p.size() === 0
      p.live() === 0
    }

    "shutdown the pool timer when it is closed" >> {
      val p = ExpiringPool(3, ReferenceType.Strong, 50.millis, () => new Object)
      p.close()

      p.timer.scheduleAtFixedRate(null, 1, 1) must
        throwA[IllegalStateException](message = "Timer already cancelled.")
    }
  }
}
