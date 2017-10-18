package nl.markvandertol.kamonstackdriver

import java.io.FileInputStream

import com.google.api.gax.core.{ CredentialsProvider, FixedCredentialsProvider }
import com.google.auth.oauth2.GoogleCredentials
import com.typesafe.config.Config

private[kamonstackdriver] object CredentialsProviderFactory {
  def fromConfig(config: Config): CredentialsProvider = {
    val method = config.getString("auth.method")
    method match {
      case "application-default" =>
        FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault)
      case "keyfile" =>
        val keyfilePath = config.getString("auth.keyfile-path")
        val credentials = GoogleCredentials.fromStream(new FileInputStream(keyfilePath))
        FixedCredentialsProvider.create(credentials)
    }
  }

}
