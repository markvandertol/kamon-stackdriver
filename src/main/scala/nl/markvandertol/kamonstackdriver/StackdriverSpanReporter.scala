package nl.markvandertol.kamonstackdriver

import java.nio.ByteBuffer

import com.google.cloud.trace.v1.{ TraceServiceClient, TraceServiceSettings }
import com.google.devtools.cloudtrace.v1.{ PatchTracesRequest, Trace, TraceSpan, Traces }
import com.google.protobuf.Timestamp
import com.typesafe.config.Config
import kamon.{ Kamon, SpanReporter }
import kamon.trace.IdentityProvider.Identifier
import kamon.trace.Span
import kamon.trace.Span.TagValue
import kamon.util.CallingThreadExecutionContext
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success, Try }

class StackdriverSpanReporter extends SpanReporter {
  private val logger = LoggerFactory.getLogger(getClass)
  private implicit def ec: ExecutionContext = CallingThreadExecutionContext

  private var projectId: String = _
  private var client: TraceServiceClient = _

  def reportSpans(spans: Seq[Span.FinishedSpan]): Unit = {
    if (spans.nonEmpty) {
      val convertedSpans = convertSpans(spans)

      val traces = Traces.newBuilder()
        .addAllTraces(convertedSpans.asJava)
        .build()

      writeTraces(traces)
    }
  }

  private def configure(globalConfig: Config): Unit = {
    val config = globalConfig.getConfig(configPrefix)
    closeClient()

    projectId = config.getString("span.google-project-id")

    val credentialsProvider = CredentialsProviderFactory.fromConfig(config)

    val settings = TraceServiceSettings.newBuilder()
    settings.setCredentialsProvider(credentialsProvider)
    client = TraceServiceClient.create(settings.build())
  }

  private def closeClient(): Unit = {
    Try {
      if (!(client eq null)) {
        client.close()
        client = null
      }
    }.failed.foreach { error =>
      logger.error("Failed to close TraceServiceClient", error)
    }
  }

  private def writeTraces(traces: Traces): Unit = {
    val request = PatchTracesRequest.newBuilder()
      .setProjectId(projectId)
      .setTraces(traces)
      .build()

    client.patchTracesCallable.futureCall(request).onComplete {
      case Success(_) => //ok
      case Failure(e) => logger.error("Failed to upload traces", e)
    }
  }

  private def convertSpans(spans: Seq[Span.FinishedSpan]): Seq[Trace] = {
    val convertedSpansWithTraceId = spans.map(convertSpan)
    val convertedSpansPerTraceId = convertedSpansWithTraceId.groupBy(_._1)
    convertedSpansPerTraceId.map {
      case (traceId, convertedSpans) =>
        Trace.newBuilder()
          .setProjectId(projectId)
          .setTraceId(traceId)
          .addAllSpans(convertedSpans.map(_._2).asJava)
          .build()
    }.toSeq
  }

  private def convertSpan(span: Span.FinishedSpan): (String, TraceSpan) = {
    val traceID = span.context.traceID.string

    val traceSpan = TraceSpan.newBuilder()
      .setStartTime(instantToTimestamp(span.from))
      .setEndTime(instantToTimestamp(span.to))
      .putAllLabels(tagsToLabels(span.tags).asJava)
      .setName(span.operationName)

    identifierToLong(span.context.spanID).foreach(traceSpan.setSpanId)
    identifierToLong(span.context.parentID).foreach(traceSpan.setParentSpanId)

    (traceID, traceSpan.build())
  }

  private def tagsToLabels(tags: Map[String, TagValue]): Map[String, String] = {
    tags.map {
      case (key, value: TagValue.Boolean) => (key, value.text)
      case (key, value: TagValue.Number) => (key, value.number.toString)
      case (key, value: TagValue.String) => (key, value.string)
    }
  }

  private def identifierToLong(identifier: Identifier): Option[Long] = {
    val firstBytes = identifier.bytes.take(8)

    if (firstBytes.length > 0) {
      Some(ByteBuffer.wrap(firstBytes).getLong.abs)
    } else {
      None
    }
  }

  def start(): Unit = {
    configure(Kamon.config())
  }

  def stop(): Unit = {
    closeClient()
  }

  def reconfigure(config: Config): Unit = {
    configure(config)
  }
}
