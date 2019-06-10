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

import clojure.lang.IExceptionInfo;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import eva.error.v1.EvaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.workiva.eva.clientservice.config.EnvironmentService;
import com.workiva.eva.clientservice.exceptions.ClientServiceException;
import com.workiva.eva.clientservice.exceptions.MimeException;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import static com.workiva.eva.clientservice.serialization.SerializerUtils.ACCEPT_HEADER;
import static com.workiva.eva.clientservice.serialization.SerializerUtils.DEFAULT_MIME_TYPE;

/** Primarily defines the ExceptionHandlers for the REST endpoints. */
@ControllerAdvice
public class RestExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

  private EnvironmentService environment;

  @Autowired
  public RestExceptionHandler(EnvironmentService environmentService) {
    this.environment = environmentService;
  }

  /**
   * These keywords are removed from the ex-data in the returned exception. Either duplicate
   * information or not deemed important
   */
  private Keyword[] exDataBlacklist = {
    // Until there is a serializer for recide errors, it is not serialized to a reconstitutable
    // form
    Keyword.intern("recide/error")
  };

  private IPersistentMap cleanExceptionData(IPersistentMap exData) {
    if (exData == null) {
      return null;
    }
    for (Keyword k : exDataBlacklist) {
      exData = exData.without(k);
    }
    return exData;
  }

  private Map<Object, Object> convertThrowableToMap(Throwable ex) {
    return convertThrowableToMap(ex, environment.exCauseNestingLimit());
  }

  private Map<Object, Object> convertThrowableToMap(Throwable ex, int causeLimit) {
    Map<Object, Object> responseMap = new HashMap<>();
    responseMap.put(Keyword.intern("message"), ex.getMessage());
    if (ex instanceof EvaException) {
      EvaException evaException = (EvaException) ex;
      responseMap.put(Keyword.intern("ex-data"), cleanExceptionData(evaException.getData()));

      Map<Object, Object> evaErrorBreakdown = new HashMap<>();
      evaErrorBreakdown.put(Keyword.intern("code"), evaException.getErrorCode().getCode());
      evaErrorBreakdown.put(
          Keyword.intern("explanation"), evaException.getErrorCode().getExplanation());
      evaErrorBreakdown.put(Keyword.intern("type"), evaException.getErrorCode().getName());
      responseMap.put(Keyword.intern("ex-info"), evaErrorBreakdown);
    } else if (ex instanceof IExceptionInfo) {
      IExceptionInfo exInfoException = (IExceptionInfo) ex;
      responseMap.put(Keyword.intern("ex-data"), cleanExceptionData(exInfoException.getData()));
    }

    // Append the exception type if it is not an EvaException
    if (!(ex instanceof EvaException)) {
      Map<Object, Object> exInfo = new HashMap<>();
      exInfo.put(Keyword.intern("type"), ex.getClass().getName());
      responseMap.put(Keyword.intern("ex-info"), exInfo);
    }

    if (ex.getCause() != null && causeLimit > 0) {
      responseMap.put(
          Keyword.intern("cause"), convertThrowableToMap(ex.getCause(), causeLimit - 1));
    }
    return responseMap;
  }

  private ResponseEntity constructExceptionResp(
      Map<Object, Object> responseMap, int httpCode, Map<String, String> headers) {
    HttpStatus code = HttpStatus.resolve(httpCode);
    if (code == null) {
      code = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return new ResponseEntity<>(SerializerUtils.serialize(headers, responseMap), code);
  }

  private Map<String, String> getRequestHeaders(HttpServletRequest request) {
    Map<String, String> headers =
        Collections.list((request).getHeaderNames())
            .stream()
            .collect(Collectors.toMap(h -> h, request::getHeader));
    return headers;
  }

  /**
   * Handle exceptions that occur from interactions with the Transactor.
   *
   * <p>These exceptions will have an EvaException as a cause which can be accessed.
   *
   * @param e The error to decode.
   * @param request The involved request.
   * @return The response / error
   */
  @ExceptionHandler(ExecutionException.class)
  ResponseEntity handleExecutionExceptions(ExecutionException e, HttpServletRequest request) {
    Map<String, String> headers = getRequestHeaders(request);

    // Verify if it the cause is an EvaException
    // If it's cause is NOT an EvaException, then it is not an exception originating from a
    // transaction
    if (e.getCause() == null || !(e.getCause() instanceof EvaException)) {
      Map<Object, Object> response = new HashMap<>();
      response.put(Keyword.intern("message"), e.getMessage());
      response.put(
          Keyword.intern("cause"), (e.getCause() == null) ? "" : e.getCause().getMessage());
      return new ResponseEntity<>(
          SerializerUtils.serialize(headers, response), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    EvaException ex = (EvaException) e.getCause();
    return constructExceptionResp(
        convertThrowableToMap(ex), (int) ex.getErrorCode().getHttpErrorCode(), headers);
  }

  /**
   * Handle exceptions that occur from interactions with the Peer.
   *
   * @param ex The error to decode.
   * @param request The involved request.
   * @return The response / error
   */
  @ExceptionHandler(EvaException.class)
  ResponseEntity handleEvaExceptions(EvaException ex, HttpServletRequest request) {
    Map<String, String> headers = getRequestHeaders(request);
    return constructExceptionResp(
        convertThrowableToMap(ex), (int) ex.getErrorCode().getHttpErrorCode(), headers);
  }

  /**
   * Handle exceptions related to MIME type / Accept Header.
   *
   * @param e The error to decode.
   * @param request The involved request.
   * @return The response / error
   */
  @ExceptionHandler(MimeException.class)
  ResponseEntity handleMimeExceptions(MimeException e, HttpServletRequest request) {
    Map<String, String> headers = getRequestHeaders(request);

    // MimeType exception would typically indicate that we have a problem with
    // serializing/deserializing
    // Therefore, it doesn't make sense to use the same headers, default to EDN
    headers.put(ACCEPT_HEADER, DEFAULT_MIME_TYPE);
    LOGGER.error(e.getMessage(), e);
    return new ResponseEntity<>(SerializerUtils.serialize(headers, e.getMessage()), e.getStatus());
  }

  @ExceptionHandler(ClientServiceException.class)
  ResponseEntity handleClientServiceExceptions(
      ClientServiceException e, HttpServletRequest request) {
    Map<String, String> headers = getRequestHeaders(request);

    LOGGER.error(e.getMessage(), e);
    Map<Object, Object> response = new HashMap<>();
    response.put(Keyword.intern("message"), e.getMessage());
    return new ResponseEntity<>(SerializerUtils.serialize(headers, response), e.getStatus());
  }

  /**
   * Catch all exception handler for any uncaught exceptions.
   *
   * @param e The error to decode.
   * @param request The involved request.
   * @return The response / error
   */
  @ExceptionHandler(Throwable.class)
  ResponseEntity handleUncaughtExceptions(Exception e, HttpServletRequest request) {
    Map<String, String> headers = getRequestHeaders(request);

    LOGGER.error("An uncaught or unknown exception occurred", e);
    Map<Object, Object> response = new HashMap<>();
    response.put(Keyword.intern("message"), e.getMessage());
    return new ResponseEntity<>(
        SerializerUtils.serialize(headers, response), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
