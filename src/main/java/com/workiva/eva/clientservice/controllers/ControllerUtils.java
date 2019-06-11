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

import com.codahale.metrics.MetricRegistry;
import eva.error.v1.EvaException;
import ichnaie.TracingContext;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.analytics.Telemetry;
import com.workiva.eva.clientservice.analytics.Tracing;
import com.workiva.eva.clientservice.config.EnvironmentService;
import com.workiva.eva.clientservice.controllers.v1.PeerControllerV1;
import com.workiva.eva.clientservice.exceptions.ExceptionUtils;
import com.workiva.eva.clientservice.reference.Reference;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Utilities for Peer Controllers. */
public class ControllerUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ControllerUtils.class);
  private static EnvironmentService environment = EnvironmentService.get();

  /**
   * Log data about the request.
   *
   * @param call The call being executed.
   * @param correlationId The correlation id.
   * @param params The parameters that are meaningful for the request.
   * @param func The function to run.
   * @return Returns the request response.
   * @throws Exception Thrown on any error.
   */
  public static String logRequestResponse(
      RequestContext parentCtx,
      String call,
      String correlationId,
      Map<String, Object> params,
      TrackingFunction func)
      throws Throwable {

    String output;
    long startTime = System.currentTimeMillis();
    RequestContext ctx = parentCtx.startSpanFromHeaders(parentCtx.getHttpHeaders(), call);
    ctx.setSpanTag(Tracing.CORRELATION_ID_TAG, correlationId);

    try (TracingContext tc = new TracingContext(GlobalTracer.get(), ctx.getSpan()); ) {
      if (environment.logParams() && params != null) {
        params.forEach((name, value) -> MDC.put(name, value.toString()));
        LOGGER.debug("Parameters for preceding call to Peer library");
      }

      LOGGER.info("Started processing {}", call);
      try (RequestContext childContext = ctx.startSpan("startEvaCall")) {
        output = func.run(ctx).toString();
        if (environment.logParams()) {
          LOGGER.debug("{}: Peer library result: {}", call, output);
        }
      }
    } catch (ExecutionException ex) {
      if (ex.getCause() instanceof EvaException) {
        ExceptionUtils.logEvaRelatedException(
            ctx, "Transactor exception encountered", (EvaException) ex.getCause());
        throw ex;
      } else {
        ExceptionUtils.logGenericException(
            ctx, String.format("Unexpected exception occurred while executing %s", call), ex);
        throw ex;
      }
    } catch (EvaException ex) {
      ExceptionUtils.logEvaRelatedException(ctx, "Eva Peer exception encountered", ex);
      throw ex;
    } catch (Exception ex) {
      ExceptionUtils.logGenericException(
          ctx, String.format("Unexpected exception occurred while executing %s", call), ex);
      throw ex;
    } finally {
      // RequestContext not in try-with-resources until we switch to jdk 9, must manually close
      ctx.close();
      long endTime = System.currentTimeMillis();
      long ms = endTime - startTime;

      Telemetry.updateTimer(MetricRegistry.name(PeerControllerV1.class, call), startTime, endTime);

      LOGGER.info("Completed call '{}'; in {}ms", call, ms);
      if (environment.logParams() && params != null) {
        params.forEach((name, value) -> MDC.remove(name));
      }
    }
    return output;
  }

  public static void addReferenceMetadata(Reference ref) {
    MDC.put("tenant", ref.tenant());
    MDC.put("category", ref.category());
    MDC.put("label", ref.label());
  }

  public static void removeReferenceMetadata() {
    MDC.remove("tenant");
    MDC.remove("category");
    MDC.remove("label");
  }

  public static void addQueryParamReferencesToMDC(String prefix, int count, Reference ref) {
    MDC.put(String.format("%s-tenant[%d]", prefix, count), ref.tenant());
    MDC.put(String.format("%s-category[%d]", prefix, count), ref.category());
    MDC.put(String.format("%s-label[%d]", prefix, count), ref.label());
  }

  public static void removeQueryParamReferenceFromMDC(int[] referenceCounters) {
    for (int i = 0; i < referenceCounters[0]; i++) {
      MDC.remove(String.format("db-tenant[%d]", i));
      MDC.remove(String.format("db-category[%d]", i));
      MDC.remove(String.format("db-label[%d]", i));
    }
    for (int i = 0; i < referenceCounters[1]; i++) {
      MDC.remove(String.format("conn-tenant[%d]", i));
      MDC.remove(String.format("conn-category[%d]", i));
      MDC.remove(String.format("conn-label[%d]", i));
    }
  }
}
