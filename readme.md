#Kamon-Stackdriver

Kamon-Stackdriver is a library to report metrics collected by [Kamon](https://github.com/kamon-io/Kamon) to
[Google Stackdriver](https://cloud.google.com/stackdriver/). It supports both
[Trace](https://cloud.google.com/trace/docs/) and [Monitoring](https://cloud.google.com/monitoring/docs/).

### Getting Started

Supported releases and dependencies are shown below.

| kamon  | status | jdk  | scala            | google-cloud-monitoring | google-cloud-trace |
|:------:|:------:|:----:|:----------------:|:----------------:|:----------------:|
|  1.1.2 | unstable | 1.8+ | 2.10, 2.11, 2.12  | 0.46.0-beta | 0.46.0-beta

This library isn't available in as a JAR in a repository at this phase.

### Kamon Configuration
The following Kamon configuration is recommended:
```
kamon {
  metric {
    # Stackdriver accepts at most a tick every minute
    tick-interval = 1 minute
  }
  trace {
    # The Stackdriver rate limit is at a 1000 requests per 100 seconds, so
    # sending frequently isn't a problem.
    tick-interval = 2 seconds

    # Make the identifiers compatible with what Stackdriver Trace expects.
    identity-provider = "nl.markvandertol.kamonstackdriver.SpanIdentityProvider"
  }
}
```

### Library configuration

See `reference.conf`. In all cases the `kamon.metric.resource` property has to be updated to reflect for what resource
you're collecting metrics.


## License

This software is licensed under the Apache 2 license, quoted below.

```Copyright © 2017 Mark van der Tol

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    [http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```

This project isn't associated with Google, Stackdriver or Kamon.