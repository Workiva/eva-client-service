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

package com.workiva.eva.clientservice.filters;

import clojure.lang.Keyword;
import com.google.gson.JsonSyntaxException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.workiva.eva.clientservice.exceptions.security.AuthException;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Generic top-level filter to catch exceptions and tailor responses based on them. */
public class ExceptionHandlerFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerFilter.class);

  private Map<String, String> getRequestHeaders(HttpServletRequest request) {
    Map<String, String> headers =
        Collections.list((request).getHeaderNames())
            .stream()
            .collect(Collectors.toMap(h -> h, request::getHeader));
    return headers;
  }

  @Override
  public void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (RuntimeException e) {
      LOGGER.error("Exception encountered during authentication flow", e);

      Map<String, String> headers = getRequestHeaders(request);
      MimeType mimeType = SerializerUtils.getMimeType(headers);
      response.setContentType(mimeType.toString());
      Map<Object, Object> responseMap = new HashMap<>();

      if (e instanceof ExpiredJwtException) {
        responseMap.put(Keyword.intern("message"), e.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(SerializerUtils.serialize(headers, responseMap));
      } else if (e instanceof SignatureException) {
        responseMap.put(Keyword.intern("message"), e.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(SerializerUtils.serialize(headers, responseMap));
      } else if (e instanceof IllegalArgumentException && e.getMessage().contains("base64")) {
        responseMap.put(Keyword.intern("message"), "Malformed JWT, not valid Base64 Encoding");
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.getWriter().write(SerializerUtils.serialize(headers, responseMap));
      } else if (e instanceof JsonSyntaxException) {
        responseMap.put(Keyword.intern("message"), "Malformed JWT, not valid JSON");
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.getWriter().write(SerializerUtils.serialize(headers, responseMap));
      } else if (e instanceof AuthException) {
        responseMap.put(Keyword.intern("message"), e.getMessage());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.getWriter().write(SerializerUtils.serialize(headers, responseMap));
      } else {
        responseMap.put(Keyword.intern("message"), e.getMessage());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.getWriter().write(SerializerUtils.serialize(headers, responseMap));
      }
    }
  }
}
