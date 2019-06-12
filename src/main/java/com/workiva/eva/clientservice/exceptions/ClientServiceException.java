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

import org.springframework.http.HttpStatus;

/** Caused when there is an eva exception. */
public class ClientServiceException extends RuntimeException {

  /** Holds the status value. */
  private final HttpStatus status;

  /**
   * Create the eva exception.
   *
   * @param cause The cause of the exception.
   * @param format The exception format.
   * @param params The format parameters.
   */
  public ClientServiceException(
      HttpStatus status, Throwable cause, String format, Object... params) {
    super(format == null || format.isEmpty() ? "" : String.format(format, params), cause);
    this.status = status;
  }

  /**
   * Create the eva exception.
   *
   * @param cause The cause of the exception.
   * @param format The exception format.
   * @param params The format parameters.
   */
  public ClientServiceException(Throwable cause, String format, Object... params) {
    this(HttpStatus.BAD_REQUEST, cause, format, params);
  }

  /**
   * Create the eva exception.
   *
   * @param format The exception format.
   * @param params The format parameters.
   */
  public ClientServiceException(HttpStatus status, String format, Object... params) {
    this(status, null, format, params);
  }

  /**
   * Get the status.
   *
   * @return Returns the status.
   */
  public HttpStatus getStatus() {
    return this.status;
  }
}
