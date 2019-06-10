// Copyright 2018-2019 Workiva Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.workiva.eva.clientservice.analytics;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.util.GlobalTracer;

/** Handles all Tracing related functionality. */
public class Tracing {

  public static String CORRELATION_ID_TAG = "correlation.id";

  /**
   * Initializes tracing.
   *
   * @param tracerType The tracer type.
   */
  public static void setUp(String tracerType) {
    if ("jaeger".equals(tracerType)) {
      GlobalTracer.register(createJaegerTracer());
    }
  }

  public static JaegerTracer createJaegerTracer() {
    SamplerConfiguration samplerConfig =
        SamplerConfiguration.fromEnv().withType("const").withParam(1);
    ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv();
    Configuration config =
        new Configuration("eva-client-service")
            .withSampler(samplerConfig)
            .withReporter(reporterConfig);
    return config.getTracer();
  }
}
