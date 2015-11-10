package io.github.andrebeat.pool

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

trait TestHelper {
  val cores = Runtime.getRuntime.availableProcessors
  val timeFactor = 8 / math.min(8, cores)

  def sleep(d: Duration) = Thread.sleep((d * timeFactor).toMillis)
  def await[A](f: Future[A], d: Duration) = Await.result(f, d * timeFactor)
}
