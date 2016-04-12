package io.github.andrebeat.pool

import scala.annotation.tailrec
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Properties

trait TestHelper {
  val timeDilationFactor = Properties.envOrNone("TIME_DILATION_FACTOR").map(_.toInt).getOrElse(1)

  @tailrec final def sleep(d: Duration): Unit =
    if (d > 1.millis) {
      Thread.sleep((1.millis * timeDilationFactor).toMillis)
      Thread.`yield`()
      sleep(d - 1.millis)
    }
  def await[A](f: Future[A], d: Duration) = Await.result(f, d * timeDilationFactor)
}
