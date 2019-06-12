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
import eva.Connection;
import eva.Util;
import org.junit.Assert;
import org.junit.Test;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.edn.Transactions;
import com.workiva.eva.clientservice.edn.Queries;
import com.workiva.eva.clientservice.peer.TestPeerRepository;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.reference.SnapshotReference;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.workiva.eva.clientservice.TestUtils.CATEGORY;
import static com.workiva.eva.clientservice.TestUtils.LABEL;
import static com.workiva.eva.clientservice.TestUtils.TENANT;

public class PeerFunctionsTest {

  static TestPeerRepository peerRepository = new TestPeerRepository();
  static TestEnvironmentService environmentService = new TestEnvironmentService(true, true, 1);

  static Map<String, String> headers =
      new HashMap<String, String>() {
        {
          put(SerializerUtils.ACCEPT_HEADER, SerializerUtils.DEFAULT_MIME_TYPE);
        }
      };
  static final RequestContext testContext =
      new RequestContext.Builder().httpHeaders(headers).build();

  @Test
  public void queryInlineTest() {
    SnapshotReference ref = new SnapshotReference(TENANT, CATEGORY, LABEL, null, false);
    PersistentVector params = PersistentVector.create(ref);

    PersistentVector result =
        (PersistentVector)
            PeerFunctions.query(peerRepository, Queries.getInstalledIdents, params, testContext);
    Assert.assertTrue(result.length() > 0);
  }

  @Test
  public void queryInlineAsOfTest() {
    SnapshotReference ref = new SnapshotReference(TENANT, CATEGORY, LABEL, null, false);
    PersistentVector params = PersistentVector.create(ref);

    Map<String, String> headers =
        new HashMap<String, String>() {
          {
            put(SerializerUtils.ACCEPT_HEADER, SerializerUtils.DEFAULT_MIME_TYPE);
          }
        };

    PersistentVector resultPreTx =
        (PersistentVector)
            PeerFunctions.query(peerRepository, Queries.getInstalledIdents, params, testContext);
    Assert.assertTrue(resultPreTx.length() > 0);

    ConnectionReference connRef = new ConnectionReference(TENANT, CATEGORY, LABEL);
    Connection conn = peerRepository.getConnection(testContext, connRef);
    conn.transact((List) Util.read(Transactions.defaultSchemaUniqueTitles));

    PersistentVector resultPostTx =
        (PersistentVector)
            PeerFunctions.query(peerRepository, Queries.getInstalledIdents, params, testContext);
    Assert.assertTrue(resultPostTx.length() > 0);
    Assert.assertTrue(resultPostTx.length() > resultPreTx.length());
  }
}
