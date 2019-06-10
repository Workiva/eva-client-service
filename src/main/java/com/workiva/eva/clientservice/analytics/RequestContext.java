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

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import com.workiva.eva.clientservice.reference.ConnectionReference;

import java.util.Map;

/**
 * Object that can be propagated throughout the client-service. Holds onto data we want to pass,
 * such as the current span or the http headers
 */
public class RequestContext implements AutoCloseable {

  private static final String SPAN_PREFIX = "eva-client-service/";
  private Tracer tracer = GlobalTracer.get();

  // Propagated Fields
  private Span activeSpan = null;
  private Map<String, String> httpHeaders;

  private RequestContext(Builder b) {
    this.activeSpan = b.activeSpan;
    this.httpHeaders = b.httpHeaders;
  }

  @Override
  public void close() {
    activeSpan.finish();
  }

  public Span getSpan() {
    return activeSpan;
  }

  public Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }

  public void setSpanTag(String tag, Number val) {
    if (activeSpan != null) {
      activeSpan.setTag(tag, val);
    }
  }

  public void setSpanTag(String tag, String val) {
    if (activeSpan != null) {
      activeSpan.setTag(tag, val);
    }
  }

  public void setSpanTag(String tag, boolean val) {
    if (activeSpan != null) {
      activeSpan.setTag(tag, val);
    }
  }

  public void setErrorTags() {
    if (this.getSpan() != null) {
      this.getSpan().setTag(Tags.SAMPLING_PRIORITY.getKey(), (short) 1);
      this.getSpan().setTag(Tags.ERROR.getKey(), true);
    }
  }

  public void appendConnectionTags(String tenant, String category, String label) {
    if (activeSpan != null) {
      setSpanTag("tenant", tenant);
      setSpanTag("category", category);
      setSpanTag("label", label);
    }
  }

  public void appendConnectionTags(ConnectionReference ref) {
    appendConnectionTags(ref.tenant(), ref.category(), ref.label());
  }

  public RequestContext startSpan(String opName) {
    Tracer.SpanBuilder spanBuilder = tracer.buildSpan(String.format("%s%s", SPAN_PREFIX, opName));
    if (activeSpan != null) {
      spanBuilder.asChildOf(activeSpan);
    }
    return new Builder().activeSpan(spanBuilder.start()).httpHeaders(this.httpHeaders).build();
  }

  /**
   * Creates a span given the headers from an Http request.
   *
   * @param headers The headers from which to extract a span context.
   * @param name The name of the span being created.
   * @return A span that is the child of the span context in the headers.
   */
  public RequestContext startSpanFromHeaders(Map<String, String> headers, String name) {

    Tracer.SpanBuilder spanBuilder = tracer.buildSpan(String.format("%s%s", SPAN_PREFIX, name));
    SpanContext context =
        GlobalTracer.get().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    if (context != null) {
      spanBuilder.asChildOf(context);
    }

    return new Builder().activeSpan(spanBuilder.start()).httpHeaders(this.httpHeaders).build();
  }

  /** Builder class for {@link RequestContext}. */
  public static class Builder {
    private Span activeSpan;
    private Map<String, String> httpHeaders;

    public Builder activeSpan(Span activeSpan) {
      this.activeSpan = activeSpan;
      return this;
    }

    public Builder httpHeaders(Map<String, String> httpHeaders) {
      this.httpHeaders = httpHeaders;
      return this;
    }

    public RequestContext build() {
      return new RequestContext(this);
    }
  }
}
