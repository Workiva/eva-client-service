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

import eva.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.functions.PeerFunction;
import com.workiva.eva.clientservice.functions.PeerFunctionCollection;
import com.workiva.eva.clientservice.peer.PeerRepository;

/** Holds the collection of functions from the Eva Database API. */
@PeerFunctionCollection
public class DatabaseFunctions {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseFunctions.class);

  /**
   * Returns the entity id associated with a symbolic keyword, or the id itself if passed.
   *
   * @param repo The repository to examine.
   * @param db The database object.
   * @param ident The ident object.
   * @return Returns the entity id.
   */
  @PeerFunction("entid")
  public static Object entid(
      PeerRepository repo, Object db, Object ident, RequestContext parentCtx) {
    try (RequestContext ctx = parentCtx.startSpan("inline-function/entid")) {
      LOGGER.info("Processing inline function - entid");
      Database realDb = repo.getSnapshot(parentCtx, db);
      return realDb.entid(ident);
    }
  }

  /**
   * Returns the keyword-identifier associated with an id.
   *
   * @param idOrKey entity-id or keyword
   * @return keyword identifier or nil if doesn't exists
   */
  @PeerFunction("ident")
  public static Object ident(
      PeerRepository repo, Object db, Object idOrKey, RequestContext parentCtx) {
    try (RequestContext ctx = parentCtx.startSpan("inline-function/ident")) {
      LOGGER.info("Processing inline function - ident");
      Database realDb = repo.getSnapshot(parentCtx, db);
      return realDb.ident(idOrKey);
    }
  }
}
