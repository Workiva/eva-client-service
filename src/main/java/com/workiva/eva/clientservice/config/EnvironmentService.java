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

package com.workiva.eva.clientservice.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service to handle all environment variables and defaults. */
@Service
public class EnvironmentService implements InitializingBean {

  private static EnvironmentService instance;

  private final boolean ecsLogParams;
  private final boolean sanitizeExceptions;
  private final int exceptionCauseNestingLimit;

  @Autowired
  public EnvironmentService(
      @Value("${ECS_LOG_PARAMS:false}") boolean ecsLogParams,
      @Value("${SANITIZE_EXCEPTIONS:true}") boolean sanitizeExceptions,
      @Value("${EX_CAUSE_NEST_LIMIT:20}") int getExceptionCauseNestingLimit) {
    this.ecsLogParams = ecsLogParams;
    this.sanitizeExceptions = sanitizeExceptions;
    this.exceptionCauseNestingLimit = getExceptionCauseNestingLimit;
  }

  @Override
  public void afterPropertiesSet() {
    instance = this;
  }

  public static EnvironmentService get() {
    return instance;
  }

  public boolean logParams() {
    return ecsLogParams;
  }

  public boolean sanitizeExceptions() {
    return sanitizeExceptions;
  }

  public int exCauseNestingLimit() {
    return exceptionCauseNestingLimit;
  }
}
