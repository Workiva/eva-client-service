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

import clojure.lang.PersistentVector;
import eva.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.controllers.QueryParameters;
import com.workiva.eva.clientservice.exceptions.InlineFunctionException;
import com.workiva.eva.clientservice.functions.PeerFunction;
import com.workiva.eva.clientservice.functions.PeerFunctionCollection;
import com.workiva.eva.clientservice.peer.PeerRepository;
import com.workiva.eva.clientservice.reference.SnapshotReference;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.util.HashMap;
import java.util.Map;

/** Holds the collection of functions from the Eva Peer API. */
@PeerFunctionCollection
public class PeerFunctions {

  private static final Logger LOGGER = LoggerFactory.getLogger(PeerFunctions.class);

  /**
   * Returns the latest tx-num that the Connection has updated its local state to match.
   *
   * @param repo The repository to examine.
   * @param query The datalog query to execute.
   * @param args The parameters to be included into the query.
   * @return the latest known tx-num on this connection.
   */
  @PeerFunction("query")
  public static Object query(
      PeerRepository repo, Object query, PersistentVector args, RequestContext parentCtx) {
    try (RequestContext ctx = parentCtx.startSpan("inline-function/query")) {
      LOGGER.info("Processing inline function - query");
      if (args.size() < 1) {
        throw new InlineFunctionException(
            "query", "No snapshot reference provided alongside inline query.");
      }
      Map<String, String> rawParams = new HashMap<String, String>();
      Integer i = 0;
      SnapshotReference ref = null;
      for (Object arg : args) {
        if (arg instanceof SnapshotReference) {
          ref = (SnapshotReference) arg;
          rawParams.put(i.toString(), ref.toString());
        } else {
          rawParams.put(i.toString(), arg.toString());
        }
        i++;
      }
      QueryParameters params = new QueryParameters();
      params.setP(rawParams);
      return Peer.query(
          SerializerUtils.deserialize(ctx.getHttpHeaders(), (String) query),
          params.all(ctx, repo, ref.tenant(), ref.category()));
    }
  }
}
