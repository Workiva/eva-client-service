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

package com.workiva.eva.clientservice.reference;

import clojure.lang.Keyword;
import eva.Connection;
import eva.Database;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.serialization.EdnSerializer;

import java.util.HashMap;
import java.util.Map;

/** Defines a snapshot reference. */
public class SnapshotReference extends ConnectionReference {

  private static final Keyword TENANT = Keyword.intern("tenant");
  private static final Keyword CATEGORY = Keyword.intern("category");
  private static final Keyword LABEL = Keyword.intern("label");

  @Deprecated private static final Keyword AS_OF = Keyword.intern("as-of");

  private static final Keyword AS_OF_T = Keyword.intern("as-of-t");
  private static final Keyword BASIS_T = Keyword.intern("basis-t");
  private static final Keyword SNAPSHOT_T = Keyword.intern("snapshot-t");

  private static final Keyword HISTORIC = Keyword.intern("historic?");
  private static final Keyword SINCE_T = Keyword.intern("since");

  private final Map<Keyword, Object> referenceContents = new HashMap<>();

  /** Holds the as-of reference. */
  private Long asOf;

  /** Whether or not syncDB should be called. */
  private Boolean syncDb;

  // TODO - Both of these are not yet used
  private boolean isHistoric = false;
  private Long since;

  /**
   * Create an instance of the snapshot reference.
   *
   * @param tenant The tenant to reference.
   * @param category The category to reference.
   * @param label The subcategory to reference.
   * @param asOf The asOf reference.
   */
  public SnapshotReference(
      String tenant, String category, String label, Long asOf, Boolean syncDb) {
    super(tenant, category, label);
    this.asOf = asOf;
    this.syncDb = syncDb;
    referenceContents.put(TENANT, tenant);
    referenceContents.put(CATEGORY, category);
    referenceContents.put(LABEL, label);
    referenceContents.put(AS_OF, asOf);
  }

  /**
   * Create an instance of the snapshot reference.
   *
   * @param reference The ConnectionReference to deserializeEdn the reference from.
   * @param asOf The asOf reference.
   */
  SnapshotReference(ConnectionReference reference, Long asOf, Boolean syncDb) {
    this(reference.tenant(), reference.category(), reference.label(), asOf, syncDb);
  }

  SnapshotReference(String tenant, String category, String label, boolean syncDb, Database db) {
    // NOTE - which value from the snapshot should be used to set the AS_OF since it is nil if it is
    // the latest?
    this(tenant, category, label, db.snapshotT(), syncDb);
    referenceContents.put(AS_OF_T, db.asOfT());
    referenceContents.put(BASIS_T, db.basisT());
    referenceContents.put(SNAPSHOT_T, db.snapshotT());
  }

  /**
   * Get a database snapshot.
   *
   * @return Returns the database snapshot.
   */
  public Database getSnapshot(RequestContext parentCtx, Connection conn) {
    try (RequestContext ctx = parentCtx.startSpan("getSnapshot")) {
      Database db = syncDb ? conn.syncDb() : conn.db();
      if (this.asOf != null) {
        db = db.asOf(this.asOf);
      }
      referenceContents.put(AS_OF_T, db.asOfT());
      referenceContents.put(BASIS_T, db.basisT());
      referenceContents.put(SNAPSHOT_T, db.snapshotT());
      return db;
    }
  }

  public Long asOf() {
    return asOf;
  }

  public Boolean syncDb() {
    return syncDb;
  }

  @Override
  public String toString() {
    String snapshotRef = ReferenceUtils.SNAPSHOT_REFERENCE_TAG;
    // TODO - this is explicitly EDN, once JSON support is added, we need to modify this and pass in
    // the headers
    EdnSerializer serializer = new EdnSerializer();
    return String.format("#%s %s", snapshotRef, serializer.serialize(null, referenceContents));
  }
}
