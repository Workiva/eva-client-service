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

package com.workiva.eva.clientservice.exceptions;

import clojure.lang.IPersistentMap;
import eva.error.v1.EvaErrorCode;
import eva.error.v1.EvaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.EnvironmentService;

/** Helper methods to sanitize and log EVA exceptions. */
public class ExceptionUtils {

  private static EnvironmentService environment = EnvironmentService.get();
  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionUtils.class);
  private static final IPersistentMap defaultSuppressionSettings =
      recide.sanex.Utils.createSuppressionMap(true, false, true, false, true);

  // Removes data and messages from exceptions to prepare them for logging
  private static Throwable recursivelySanitize(Throwable ex) {
    if (ex == null) {
      return null;
    }
    Throwable newException = new Throwable("Sanitized", recursivelySanitize(ex.getCause()));
    newException.setStackTrace(ex.getStackTrace());
    return newException;
  }

  public static void logGenericException(RequestContext ctx, String message, Throwable ex) {
    ctx.setErrorTags();
    // If we are in an environment where exceptions should be sanitized, do so before logging them
    if (environment.sanitizeExceptions()) {
      ex = recursivelySanitize(ex);
    }
    LOGGER.error(String.format("%s", message), ex);
  }

  public static void logEvaRelatedException(RequestContext ctx, String message, EvaException ex) {
    ctx.setErrorTags();

    // If we are in an environment where exceptions should be sanitized, do so before logging them
    if (environment.sanitizeExceptions()) {
      ex = (EvaException) ex.getSanitized(defaultSuppressionSettings);
    }

    try (MDC.MDCCloseable code =
            MDC.putCloseable("eva/errorCode", String.valueOf(ex.getErrorCode().getCode()));
        MDC.MDCCloseable errorType =
            MDC.putCloseable("eva/errorType", ex.getErrorCode().getName());
        MDC.MDCCloseable errorExplanation =
            MDC.putCloseable("eva/errorExplanation", ex.getErrorCode().getExplanation());
        MDC.MDCCloseable exData = MDC.putCloseable("ex-data", ex.getData().toString()); ) {
      if (ex.getErrorCode() == EvaErrorCode.UNKNOWN_ERROR) {
        LOGGER.error("Uncategorized exception encountered");
      }
      LOGGER.error(String.format("%s", message), ex);
    }
  }
}
