package io.github.andrebeat.pool

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.Properties

trait TestHelper {
  val timeDilationFactor = Properties.envOrNone("TIME_DILATION_FACTOR").map(_.toInt).getOrElse(1)

  def sleep(d: Duration) = Thread.sleep((d * timeDilationFactor).toMillis)
  def await[A](f: Future[A], d: Duration) = Await.result(f, d * timeDilationFactor)
}
