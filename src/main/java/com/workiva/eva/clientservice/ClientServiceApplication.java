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

package com.workiva.eva.clientservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.workiva.eva.clientservice.analytics.Telemetry;
import com.workiva.eva.clientservice.analytics.Tracing;

/** Defines the application for the Eva proxy. */
@SpringBootApplication
public class ClientServiceApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientServiceApplication.class);

  /**
   * Defines the main entry point to the application.
   *
   * @param args The command line arguments.
   */
  public static void main(String[] args) {
    
    
    
    boolean telemetryDisabled =                   false;
    // TODO:
    // https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/core/env/CommandLinePropertySource.html
    for (String arg : args) {
      if (arg.startsWith("-tracing=")) {
        Tracing.setUp(arg.split("=")[1]);
      }
      if (arg.startsWith("-disableTelemetry=")) {
        telemetryDisabled = Boolean.parseBoolean(arg.split("=")[1]);
      }
    }

    if (!telemetryDisabled) {
      LOGGER.info("Enabling Telemetry");
      Telemetry.setUp();
    } else {
      LOGGER.warn(
          "Telemetry Disabled! Remove -disableTelemetry argument or set to false to re-enable.");
    }

    SpringApplication.run(ClientServiceApplication.class, args);
  }
}
