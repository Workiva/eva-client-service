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

package com.workiva.eva.clientservice.functions.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.functions.PeerFunction;
import com.workiva.eva.clientservice.functions.PeerFunctionCollection;
import com.workiva.eva.clientservice.peer.PeerRepository;

import java.util.List;

/** Holds the collection of functions that could serve useful. */
@PeerFunctionCollection
public class UtilFunctions {

  private static final Logger LOGGER = LoggerFactory.getLogger(UtilFunctions.class);

  /**
   * Returns the first item in the List.
   *
   * @param list The list.
   * @return Returns the first item in the collection.
   */
  @PeerFunction("first")
  public static Object first(PeerRepository repo, List list, RequestContext parentCtx) {
    try (RequestContext ctx = parentCtx.startSpan("inline-function/first")) {
      LOGGER.info("Processing inline function - first");
      return list.get(0);
    }
  }

  @PeerFunction("ffirst")
  public static Object ffirst(PeerRepository repo, List list, RequestContext parentCtx) {
    try (RequestContext ctx = parentCtx.startSpan("inline-function/first")) {
      LOGGER.info("Processing inline function - ffirst");
      return ((List) list.get(0)).get(0);
    }
  }
}
