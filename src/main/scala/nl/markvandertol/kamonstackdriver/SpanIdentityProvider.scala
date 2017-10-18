package nl.markvandertol.kamonstackdriver

import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom

import kamon.trace.IdentityProvider
import kamon.trace.IdentityProvider.{Generator, Identifier}
import kamon.util.HexCodec

import scala.util.Try

class SpanIdentityProvider extends IdentityProvider {
  protected val traceCompatibleGenerator: Generator = new Generator {
    def generate(): Identifier = {
      val data = ByteBuffer.wrap(new Array[Byte](16))
      val random1 = ThreadLocalRandom.current().nextLong()
      val random2 = ThreadLocalRandom.current().nextLong()
      data.putLong(random1)
      data.putLong(random2)

      Identifier(HexCodec.toLowerHex(random1) + HexCodec.toLowerHex(random2), data.array())
    }

    def from(string: String): Identifier = Try {
      val identifierLong1 = HexCodec.lowerHexToUnsignedLong(string.take(8))
      val identifierLong2 = HexCodec.lowerHexToUnsignedLong(string.substring(8))

      val data = ByteBuffer.allocate(16)
      data.putLong(identifierLong1)
      data.putLong(identifierLong2)

      Identifier(string, data.array())
    } getOrElse (IdentityProvider.NoIdentifier)

    def from(bytes: Array[Byte]): Identifier = Try {
      val buffer = ByteBuffer.wrap(bytes)
      val identifierLong = buffer.getLong

      Identifier(HexCodec.toLowerHex(identifierLong), bytes)
    } getOrElse (IdentityProvider.NoIdentifier)
  }

  def traceIdGenerator(): Generator = traceCompatibleGenerator

  def spanIdGenerator(): Generator = IdentityProvider.Default().spanIdGenerator()
}

