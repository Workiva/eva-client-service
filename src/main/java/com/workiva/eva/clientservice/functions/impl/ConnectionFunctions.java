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

import eva.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.functions.PeerFunction;
import com.workiva.eva.clientservice.functions.PeerFunctionCollection;
import com.workiva.eva.clientservice.peer.PeerRepository;

/** Holds the collection of functions from the Eva Peer API. */
@PeerFunctionCollection
public class ConnectionFunctions {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionFunctions.class);

  /**
   * Returns the latest tx-num that the Connection has updated its local state to match.
   *
   * @param repo The repository to examine.
   * @return the latest known tx-num on this connection.
   */
  @PeerFunction("latestT")
  public static Object latestT(PeerRepository repo, Object ref, RequestContext parentCtx) {
    try (RequestContext ctx = parentCtx.startSpan("inline-function/latestT")) {
      LOGGER.info("Processing inline function - latestT");
      Connection realConn = repo.getConnection(ctx, ref);
      return realConn.latestT();
    }
  }
}
