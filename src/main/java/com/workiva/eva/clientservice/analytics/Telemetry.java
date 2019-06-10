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

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import eva.Alpha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/** Handles all Telemetry related functionality. */
public class Telemetry {
  private static final Logger LOGGER = LoggerFactory.getLogger(Telemetry.class);
  public static MetricRegistry metrics = new MetricRegistry();
  private static ConsoleReporter reporter;

  /** Initializes telemetry. */
  public static void setUp() {
    metrics.register("peer", Alpha.internalMetricRegistry());
    registerJvmMetrics();

    reporter = ConsoleReporter.forRegistry(metrics).build();
    reporter.start(60, TimeUnit.SECONDS);
  }

  /**
   * Updates the value of a Histogram given the name and start time.
   *
   * @param name The name of the histogram.
   * @param start The start of the histogram.
   */
  public static void updateTimer(String name, long start) {
    updateTimer(name, start, System.currentTimeMillis());
  }

  /**
   * Updates the value of a Histogram given the name and start time.
   *
   * @param name The name of the histogram.
   * @param start The start time for the histogram.
   * @param end The end time for the histogram.
   */
  public static void updateTimer(String name, long start, long end) {
    metrics.timer(name).update(end - start, TimeUnit.MILLISECONDS);
  }

  /**
   * Registers a gauge if it is not already registered.
   *
   * @param name The name of the gauge.
   * @param gauge The gauge itself.
   */
  public static void registerGauge(String name, Gauge gauge) {
    if (!metrics.getGauges().containsKey(name)) {
      metrics.register(name, gauge);
    } else {
      LOGGER.warn("{} is already a registered gauge.", name);
    }
  }

  /** Registers jvm memory and garbage collector metrics. */
  private static void registerJvmMetrics() {
    MetricSet jvmMemoryMetrics = new MemoryUsageGaugeSet();
    MetricSet jvmGcMetrics = new GarbageCollectorMetricSet();
    metrics.registerAll(jvmMemoryMetrics);
    metrics.registerAll(jvmGcMetrics);
  }
}
