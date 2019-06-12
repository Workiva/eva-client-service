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
import com.workiva.eva.clientservice.exceptions.ClientServiceException;
import com.workiva.eva.clientservice.peer.PeerRepository;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.reference.ReferenceUtils;
import com.workiva.eva.clientservice.reference.SnapshotReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Defines the query request. */
public class QueryParameters {

  /** Holds the fields. */
  private Map<String, String> fields = new LinkedHashMap<>();

  /**
   * Get the fields collection.
   *
   * @return Returns the fields collection.
   */
  public Map<String, String> getP() {
    return fields;
  }

  /**
   * Set the fields collection.
   *
   * @param fields The fields to set.
   */
  public void setP(Map<String, String> fields) {
    this.fields = fields;
  }

  /**
   * Get all the parameters as a object collection.
   *
   * @return Return all the parameters
   */
  public Object[] all(RequestContext parentCtx, PeerRepository repo, String tenant, String category)
      throws ClientServiceException {

    List<Object> fields = new ArrayList<>();
    int dbCount = 0;
    int connCount = 0;
    for (String value : this.fields.values()) {
      Object v = ReferenceUtils.deserializeEdn(parentCtx, repo, tenant, category, value);
      if (v instanceof SnapshotReference) {
        SnapshotReference snapRef = (SnapshotReference) v;
        ControllerUtils.addQueryParamReferencesToMDC("db", dbCount++, snapRef);
        v = repo.getSnapshot(parentCtx, v);
      } else if (v instanceof ConnectionReference) {
        ConnectionReference connRef = (ConnectionReference) v;
        ControllerUtils.addQueryParamReferencesToMDC("conn", connCount++, connRef);
        v = repo.getConnection(parentCtx, v);
      }
      fields.add(v);
    }
    return fields.toArray();
  }

  /**
   * Returns the string value of this collection.
   *
   * @return Returns the string value.
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("{");
    boolean first = true;
    for (Map.Entry<String, String> e : this.fields.entrySet()) {
      if (!first) {
        builder.append(", ");
      } else {
        first = false;
      }
      builder.append("'" + e.getKey() + "' = [" + e.getValue() + "] ");
    }
    builder.append("}");

    return builder.toString();
  }
}
