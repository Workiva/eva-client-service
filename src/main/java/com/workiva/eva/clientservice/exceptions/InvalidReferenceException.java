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

/** Raised when the reference is not supported. */
public class InvalidReferenceException extends ClientServiceException {

  /** Create the unsupported mime type. */
  public InvalidReferenceException() {
    super(HttpStatus.BAD_REQUEST, "Bad reference");
  }
}
