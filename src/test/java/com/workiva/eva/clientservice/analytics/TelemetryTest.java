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

import org.junit.Assert;
import org.junit.Test;

import static com.workiva.eva.clientservice.analytics.Telemetry.metrics;

public class TelemetryTest {

  @Test
  public void testInitTelemetryObject() {
    Telemetry telem = new Telemetry();
  }

  @Test
  public void testUpdateTimerWithStartTime() {
    Telemetry.updateTimer("some-event", 123);
    Assert.assertEquals(1, metrics.timer("some-event").getCount());
  }

  @Test
  public void testUpdateTimerWithStartAndEndTime() {
    Telemetry.updateTimer("another-event", 123, 456);
    Assert.assertEquals(1, metrics.timer("another-event").getCount());
  }

  @Test
  public void testUpdateTimerWithStartTimeNotLocal() {
    Telemetry.updateTimer("some-other-event", 123);
    Assert.assertEquals(1, metrics.timer("some-other-event").getCount());
  }
}
