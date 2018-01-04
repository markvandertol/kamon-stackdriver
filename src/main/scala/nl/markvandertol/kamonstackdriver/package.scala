package nl.markvandertol

import java.time.Instant
import java.util.concurrent.Executor

import com.google.api.core.ApiFuture
import com.google.protobuf.Timestamp

import scala.concurrent.{ Future, Promise }
import scala.language.implicitConversions
import scala.util.Try

package object kamonstackdriver {

  private[kamonstackdriver] val configPrefix = "kamon.kamonstackdriver"

  private[kamonstackdriver] implicit def apiFutureToFuture[T](future: ApiFuture[T]): Future[T] = {
    val executor = new Executor {
      def execute(command: Runnable): Unit = command.run()
    }

    val promise = Promise[T]()
    future.addListener(new Runnable {
      def run(): Unit = {
        promise.complete(Try(future.get()))
      }
    }, executor)
    promise.future
  }

  private[kamonstackdriver] def instantToTimestamp(instant: Instant): Timestamp = {
    Timestamp.newBuilder()
      .setSeconds(instant.getEpochSecond)
      .setNanos(instant.getNano)
      .build()
  }
}
