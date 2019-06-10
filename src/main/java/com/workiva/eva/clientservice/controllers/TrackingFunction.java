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

import com.workiva.eva.clientservice.analytics.RequestContext;

/** Defines the functional interface for the tracking mechanism. */
@FunctionalInterface
public interface TrackingFunction {

  /**
   * Run the function.
   *
   * @return Returns the run result.
   * @throws Exception Thrown if there is an error.
   */
  Object run(RequestContext ctx) throws Throwable;
}
