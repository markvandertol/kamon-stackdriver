package nl.markvandertol.kamonstackdriver

import com.google.api.MetricDescriptor.MetricKind
import com.google.api.{ Metric, MonitoredResource }
import com.google.cloud.monitoring.v3.{ MetricServiceClient, MetricServiceSettings }
import com.google.monitoring.v3.{ CreateTimeSeriesRequest, Point, ProjectName, TimeInterval, TimeSeries, TypedValue }
import com.google.protobuf.Timestamp
import com.typesafe.config.Config
import kamon.{ Kamon, MetricReporter }
import kamon.metric.{ MetricDistribution, MetricValue, TickSnapshot }
import kamon.util.CallingThreadExecutionContext
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success, Try }

class StackdriverMetricReporter extends MetricReporter {

  private val logger = LoggerFactory.getLogger(getClass)

  private val maxTimeseriesPerRequest = 200
  private implicit def ec: ExecutionContext = CallingThreadExecutionContext

  private var client: MetricServiceClient = _

  private var projectId: String = _
  private var histogramToDistributionConverter: HistogramToDistributionConverter = _
  private var resource: MonitoredResource = _

  def reportTickSnapshot(snapshot: TickSnapshot): Unit = {
    val interval = TimeInterval.newBuilder()
      .setEndTime(timeToTimestamp(snapshot.interval.to))
      .build()

    val histogramSeries = snapshot.metrics.histograms.map(v => histogram(v, interval))
    val counterSeries = snapshot.metrics.counters.map(v => counters(v, interval))
    val gaugeSeries = snapshot.metrics.gauges.map(v => counters(v, interval))
    val minMaxSeries = snapshot.metrics.minMaxCounters.map(v => histogram(v, interval))

    val allSeries: Seq[TimeSeries] = histogramSeries ++ counterSeries ++ gaugeSeries ++ minMaxSeries

    if (allSeries.nonEmpty) {
      val allSeriesSplit = allSeries.grouped(maxTimeseriesPerRequest)

      allSeriesSplit.foreach { series =>
        val request = CreateTimeSeriesRequest.newBuilder()
          .addAllTimeSeries(series.asJava)
          .setNameWithProjectName(ProjectName.of(projectId))
          .build()
        writeSeries(request)
      }
    }
  }

  private[this] def readResource(config: Config): Unit = {
    val resourceLabels = config.getConfig("labels").entrySet().asScala.map { entry =>
      entry.getKey -> entry.getValue.unwrapped().toString
    }.toMap

    resource = MonitoredResource.newBuilder()
      .setType(config.getString("type"))
      .putAllLabels(resourceLabels.asJava)
      .build()
  }

  private[this] def writeSeries(timeSeriesRequest: CreateTimeSeriesRequest): Unit = {
    client.createTimeSeriesCallable().futureCall(timeSeriesRequest).onComplete {
      case Success(_) => //ok
      case Failure(e) => logger.error("Failed to send TimeSeries", e)
    }
  }

  private def timeToTimestamp(timeInMilliseconds: Long): Timestamp = {
    Timestamp.newBuilder()
      .setSeconds(timeInMilliseconds / 1000)
      .setNanos((timeInMilliseconds % 1000).toInt * 1000000)
      .build()
  }

  private def newTimeSeries(name: String, tags: kamon.Tags, typedValue: TypedValue, timeInterval: TimeInterval) = {
    val point = Point.newBuilder()
      .setValue(typedValue)
      .setInterval(timeInterval)
      .build()

    val fullMetricType = "custom.googleapis.com/kamon/" + name.replace('.', '/')
    val metric = Metric.newBuilder()
      .setType(fullMetricType)
      .putAllLabels(tags.asJava)
      .build()

    TimeSeries.newBuilder()
      .setMetric(metric)
      .addPoints(point)
      .setMetricKind(MetricKind.GAUGE)
      .setResource(resource)
      .build()
  }

  def histogram(v: MetricDistribution, timeInterval: TimeInterval): TimeSeries = {
    val distribution = histogramToDistributionConverter.histogramToDistribution(v.distribution.buckets, v.distribution.count)

    val typedValue = TypedValue.newBuilder()
      .setDistributionValue(distribution)
      .build()
    newTimeSeries(v.name, v.tags, typedValue, timeInterval)
  }

  def counters(v: MetricValue, timeInterval: TimeInterval): TimeSeries = {
    val typedValue = TypedValue.newBuilder()
      .setInt64Value(v.value)
      .build()

    newTimeSeries(v.name, v.tags, typedValue, timeInterval)
  }

  private def configureDistributionBuckets(config: Config): Unit = {
    val bucketType = config.getString("bucket-type")
    histogramToDistributionConverter = bucketType match {
      case "exponential" =>
        new ExponentialBucket(
          numFiniteBuckets = config.getInt("num-finite-buckets"),
          growthFactor = config.getDouble("growth-factor"),
          scale = config.getDouble("growth-factor"))
      case _ =>
        throw new IllegalArgumentException(s"Unknown bucket type: $bucketType")
    }
  }

  private def configure(globalConfig: Config): Unit = {
    val config = globalConfig.getConfig(configPrefix)
    closeClient()

    val credentialsProvider = CredentialsProviderFactory.fromConfig(config)

    projectId = config.getString("metric.google-project-id")

    configureDistributionBuckets(config.getConfig("metric.distribution"))

    readResource(config.getConfig("metric.resource"))

    val settings = MetricServiceSettings.newBuilder()
    settings.setCredentialsProvider(credentialsProvider)

    client = MetricServiceClient.create(settings.build())
  }

  private def closeClient(): Unit = {
    Try {
      if (!(client eq null)) {
        client.close()
        client = null
      }
    }.failed.foreach { error =>
      logger.error("Failed to close MetricServiceClient", error)
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
