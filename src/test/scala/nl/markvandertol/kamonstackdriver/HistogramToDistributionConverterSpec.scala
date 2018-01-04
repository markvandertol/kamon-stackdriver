package nl.markvandertol.kamonstackdriver

import kamon.metric.Bucket

class HistogramToDistributionConverterSpec extends org.specs2.mutable.Specification {
  "ExponentialBucket" >> {
    val sut = new ExponentialBucket(10, 1.5, 1.0)

    "Put small values in the first bucket" >> {
      val buckets = Vector(
        TestBucket(0, 5))

      val expected = List(5L)

      sut.histogramToDistributionValues(buckets).toList must_== expected
    }

    "Put large values in the last bucket" >> {
      val buckets = Vector(
        TestBucket(100, 5))

      val expected = Range(0, 11).map(_ => 0L) :+ 5L

      sut.histogramToDistributionValues(buckets).toList must_== expected
    }

    "Correctly interpolate values into right bucket" >> {
      val buckets = Vector(
        TestBucket(35, 5))
      val expected = List(0, 0, 0, 0,
        0, 0, 0, 0,
        0, 5)

      sut.histogramToDistributionValues(buckets).toList must_== expected
    }
  }

  "LinearBucket" >> {
    val sut = new LinearBucket(numFiniteBuckets = 10, width = 2.0, offset = 1.0)

    "Put small values in the first bucket" >> {
      val buckets = Vector(
        TestBucket(0, 5))

      val expected = List(5L)

      sut.histogramToDistributionValues(buckets).toList must_== expected
    }

    "Put large values in the last bucket" >> {
      val buckets = Vector(
        TestBucket(25, 5))

      val expected = Range(0, 11).map(_ => 0L) :+ 5L

      sut.histogramToDistributionValues(buckets).toList must_== expected
    }

    "Correctly interpolate values into right bucket" >> {
      val buckets = Vector(
        TestBucket(5, 5))
      val expected = List(0, 0, 0, 5)

      sut.histogramToDistributionValues(buckets).toList must_== expected
    }
  }
}

case class TestBucket(value: Long, frequency: Long) extends Bucket