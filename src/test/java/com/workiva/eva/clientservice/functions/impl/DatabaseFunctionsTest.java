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

import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import eva.Connection;
import eva.Peer;
import eva.Util;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.edn.Transactions;
import com.workiva.eva.clientservice.peer.TestPeerRepository;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.reference.SnapshotReference;

import java.util.List;

public class DatabaseFunctionsTest {

  static TestPeerRepository peerRepository = new TestPeerRepository();
  static TestEnvironmentService environmentService = new TestEnvironmentService(true, true, 1);
  static RequestContext testContext = new RequestContext.Builder().build();

  static SnapshotReference snapshotReference =
      new SnapshotReference("tenant", "category", "label", null, false);
  static Connection conn = peerRepository.getConnection(testContext, snapshotReference);

  @Before
  public void setUp() {
    conn.transact((List) Util.read(Transactions.defaultSchema));
  }

  @Test
  public void entIdTest() {
    SnapshotReference ref =
        new SnapshotReference("test-tenant", "test-category", "test-label", null, false);
    PersistentVector params =
        PersistentVector.create(Keyword.intern("db/ident"), Keyword.intern("db/ident"));
    Object result = DatabaseFunctions.entid(peerRepository, ref, params, testContext);
    Assert.assertTrue(result instanceof Long);
    Assert.assertEquals(3L, result);
  }

  @Test
  public void entIdAfterTxTest() {
    ConnectionReference ref =
        new ConnectionReference("test-tenant-tx", "test-category-tx", "test-label-tx");
    Connection conn = peerRepository.getConnection(testContext, ref);
    conn.transact((List) Util.read(Transactions.defaultSchemaUniqueTitles));
    PersistentVector params = PersistentVector.create(Keyword.intern("book/title"), "First Book");
    SnapshotReference dbRef =
        new SnapshotReference("test-tenant-tx", "test-category-tx", "test-label-tx", null, false);

    Assert.assertNull(DatabaseFunctions.entid(peerRepository, dbRef, params, testContext));
    peerRepository
        .getConnection(testContext, ref)
        .transact((List) Util.read(Transactions.singleBook));
    Object result = DatabaseFunctions.entid(peerRepository, ref, params, testContext);
    Assert.assertTrue(result instanceof Long);
    Assert.assertEquals(8796093023236L, result);
  }

  @Test
  public void entIdDoesntExistTest() {
    SnapshotReference ref =
        new SnapshotReference("test-tenant", "test-category", "test-label", null, false);
    PersistentVector params =
        PersistentVector.create(Keyword.intern("db/identt"), Keyword.intern("db/ident"));
    Object result = DatabaseFunctions.entid(peerRepository, ref, params, testContext);
    Assert.assertNull(result);
  }

  @Test
  public void getIdentWithEntIdTest() throws Exception {

    conn.transact((List) Util.read("[[:db/add #db/id [:db.part/tx] :db/ident :ident-with-entid]]"))
        .join();

    Object[] dbArray = {conn.db()};
    PersistentVector queryResult =
        Peer.query(Util.read("[:find ?e :in $ :where [?e :db/ident :ident-with-entid]]"), dbArray);

    long entid = (long) ((PersistentVector) queryResult.nth(0)).nth(0);

    Keyword ident =
        (Keyword)
            DatabaseFunctions.ident(this.peerRepository, snapshotReference, entid, testContext);

    Assert.assertEquals(ident, Keyword.intern("ident-with-entid"));
  }

  @Test
  public void getEntIdwithIdentTest() throws Exception {
    conn.transact((List) Util.read("[[:db/add #db/id [:db.part/tx] :db/ident :entid-with-ident]]"))
        .join();

    long entidResult =
        (long)
            DatabaseFunctions.entid(
                peerRepository, snapshotReference, Keyword.intern("entid-with-ident"), testContext);

    Object[] dbArray = {conn.db()};
    PersistentVector queryResult =
        Peer.query(Util.read("[:find ?e :in $ :where [?e :db/ident :entid-with-ident]]"), dbArray);

    long entid = (long) ((PersistentVector) queryResult.nth(0)).nth(0);
    Assert.assertEquals(entid, entidResult);
  }

  @Test
  public void getEntIdWithEntId() throws Exception {
    conn.transact((List) Util.read("[[:db/add #db/id [:db.part/tx] :db/ident :entid-with-entid]]"))
        .join();

    Object[] dbArray = {conn.db()};
    PersistentVector queryResult =
        Peer.query(Util.read("[:find ?e :in $ :where [?e :db/ident :entid-with-entid]]"), dbArray);

    long entid = (long) ((PersistentVector) queryResult.nth(0)).nth(0);

    long entidResult =
        (long) DatabaseFunctions.entid(peerRepository, snapshotReference, entid, testContext);

    Assert.assertEquals(entid, entidResult);
  }
}
