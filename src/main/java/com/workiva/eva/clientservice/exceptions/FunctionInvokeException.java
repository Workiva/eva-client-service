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

/** Defines a function invocation error. */
public class FunctionInvokeException extends ClientServiceException {

  /**
   * Create an instance of the exception.
   *
   * @param function The function throwing the error.
   * @param message The error reason.
   */
  public FunctionInvokeException(String function, String message) {
    super(HttpStatus.NOT_ACCEPTABLE, "Function: %s, Reason: %s", function, message);
  }
}
