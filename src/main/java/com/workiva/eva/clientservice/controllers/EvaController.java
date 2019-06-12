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

package com.workiva.eva.clientservice.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.workiva.eva.clientservice.peer.PeerVersion;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Defines the controller for the pull operations. */
@Controller
@RequestMapping(path = {"eva", "s/eva-client-service/eva"})
public class EvaController {

  /** Defines the LOGGER. */
  private static final Logger LOGGER = LoggerFactory.getLogger(EvaController.class);

  /** Holds the versions supported. */
  private static final List<String> versions = new ArrayList<>();

  /** Create an eva controller. */
  EvaController() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(PeerVersion.class));

    scanner
        .findCandidateComponents(EvaController.class.getPackage().getName())
        .forEach(
            (def) -> {
              try {
                Class<?> cls = ClassUtils.getDefaultClassLoader().loadClass(def.getBeanClassName());
                PeerVersion ver = cls.getAnnotation(PeerVersion.class);
                this.versions.add(ver.value());
              } catch (Exception e) {
                LOGGER.error("Unexpected error getting API versions", e);
              }
            });
  }

  /**
   * Get the list of version.
   *
   * @return Returns the versions supported.
   */
  @RequestMapping(path = "versions", method = RequestMethod.GET)
  public @ResponseBody String versions(@RequestHeader Map<String, String> headers) {
    try (MDC.MDCCloseable logPath = MDC.putCloseable("path", "versions"); ) {
      LOGGER.info("Request for API Versions '/versions'");
      return SerializerUtils.serialize(headers, versions);
    }
  }
}
