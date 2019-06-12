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

package com.workiva.eva.clientservice.peer;

import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import eva.Connection;
import eva.Database;
import eva.Peer;
import eva.catalog.client.alpha.HTTPCatalogClientImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.reference.Reference;
import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.exceptions.InvalidReferenceException;
import com.workiva.eva.clientservice.exceptions.NotFoundException;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.reference.SnapshotReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HTTPCatalogClientImpl.class, Peer.class})
public class PeerRepositoryImplTest {
  static final TestEnvironmentService environmentService =
      new TestEnvironmentService(true, true, 1);
  static final RequestContext testContext = new RequestContext.Builder().build();

  // Test Connection Object that can be used when unit-testing h2 since an actual connection
  // cannot be established and Peer.connect must be mocked
  private Connection testConnection =
      Peer.connect(
          PersistentHashMap.create(
              Keyword.intern("local"), true,
              Keyword.intern("eva.v2.storage.value-store.core/partition-id"), UUID.randomUUID(),
              Keyword.intern("eva.v2.database.core/id"), UUID.randomUUID()));

  @Test
  public void testGetLocalInMemConfig() {
    PeerRepositoryImpl peerRepository = new PeerRepositoryImpl();
    PeerKey key = new PeerKey("test-tenant", "test-category", "test-label");
    PeerRepositoryImpl.IdentifierPair pair = peerRepository.getIdentifierPair(key);
    Map config = peerRepository.getLocalInMemConfig(testContext, pair);
    Map expectedConfig =
        new HashMap() {
          {
            put(Keyword.intern("local"), true);
            put(Keyword.intern("eva.v2.storage.value-store.core/partition-id"), pair.partitionId);
            put(Keyword.intern("eva.v2.database.core/id"), pair.dbId);
          }
        };
    Assert.assertEquals(config, expectedConfig);
  }

  @Test
  public void testGetLocalConnection() {
    TestPeerRepository peerRepository = new TestPeerRepository();
    peerRepository.setCatalogUrl("LOCAL");
    ConnectionReference ref = new ConnectionReference("tenant", "cat", "sub");
    Connection conn = peerRepository.getConnection(testContext, ref);

    Assert.assertNotNull(conn);
    Assert.assertTrue(peerRepository.getConnections().containsValue(conn));
    Assert.assertTrue(conn.latestT() >= 0);
  }

  @Test
  public void testGetConnectionWithCatalog() throws Exception {
    PowerMockito.mockStatic(HTTPCatalogClientImpl.class);
    PowerMockito.mockStatic(Peer.class);
    UUID partitionId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();

    Map testConfig =
        PersistentHashMap.create(
            Keyword.intern("local"),
            true,
            Keyword.intern("eva.v2.system.peer-connection.core/id"),
            UUID.randomUUID(),
            Keyword.intern("eva.v2.database.core/id"),
            databaseId,
            Keyword.intern("eva.v2.storage.value-store.core/partition-id"),
            partitionId,
            Keyword.intern("eva.v2.storage.block-store.types/storage-type"),
            Keyword.intern("eva.v2.storage.block-store.types/sql"),
            Keyword.intern("eva.catalog.common.alpha.config/tenant"),
            "tenant",
            Keyword.intern("eva.catalog.common.alpha.config/category"),
            "cat",
            Keyword.intern("eva.catalog.common.alpha.config/label"),
            "sub",
            Keyword.intern("eva.v2.storage.block-store.peer.sql/db-spec"),
            PersistentHashMap.create(
                Keyword.intern("classname"), "org.h2.Driver",
                Keyword.intern("subprotocol"), "h2",
                Keyword.intern("subname"), "./dbs/unit-tests-db.h2",
                Keyword.intern("user"), "sa"));
    PowerMockito.doReturn(testConfig)
        .when(
            HTTPCatalogClientImpl.class,
            "requestFlatPeerConfig",
            "NOTLOCAL",
            "tenant",
            "cat",
            "sub");
    // H2 files require the catalog service running,
    PowerMockito.doReturn(testConnection).when(Peer.class, "connect", testConfig);

    TestPeerRepository peerRepository = new TestPeerRepository();
    peerRepository.setCatalogUrl("NOTLOCAL");

    ConnectionReference ref = new ConnectionReference("tenant", "cat", "sub");
    Connection conn = peerRepository.getConnection(testContext, ref);

    Assert.assertTrue(conn.latestT() >= 0);
  }

  @Test
  public void testReuseConnectionIfCreatedAlready() {
    TestPeerRepository peerRepository = new TestPeerRepository();
    peerRepository.setCatalogUrl("LOCAL");

    ConnectionReference ref = new ConnectionReference("tenant", "cat", "sub");
    Connection conn = peerRepository.getConnection(testContext, ref);

    ref = new ConnectionReference("tenant", "cat", "sub");
    Connection conn2 = peerRepository.getConnection(testContext, ref);

    Assert.assertEquals(conn, conn2);

    Assert.assertTrue(peerRepository.getConnections().containsValue(conn));
    Assert.assertEquals(peerRepository.getConnections().size(), 1);
  }

  @Test(expected = NotFoundException.class)
  public void testInvalidConnectionThrowsNotFoundException() {
    PeerRepositoryImpl peerRepository = new PeerRepositoryImpl();
    ConnectionReference ref = new ConnectionReference(null, null, null);
    peerRepository.getConnection(testContext, ref);
  }

  @Test(expected = InvalidReferenceException.class)
  public void testInvalidReferenceConnectionThrown() {
    PeerRepositoryImpl peerRepository = new PeerRepositoryImpl();
    String ref = "this is not at all a valid connection reference object";
    peerRepository.getConnection(testContext, ref);
  }

  @Test
  public void testGetSnapshotWithConnectionReference() {
    PeerRepositoryImpl peerRepository = new PeerRepositoryImpl();
    Reference ref = new ConnectionReference("tenant", "cat", "sub");
    Database db = peerRepository.getSnapshot(testContext, ref);

    // TODO: Can probably be checked on Db
    Assert.assertNotNull(db);
  }

  @Test
  public void testGetSnapshotWithSnapshotReference() {
    PeerRepositoryImpl peerRepository = new PeerRepositoryImpl();

    // TODO: probably have to get an actual asOf value instead of just 0
    Reference ref = new SnapshotReference("tenant", "cat", "sub", 0L, false);
    Database db = peerRepository.getSnapshot(testContext, ref);

    // TODO: Can probably be checked on Db
    Assert.assertNotNull(db);
  }

  @Test(expected = InvalidReferenceException.class)
  public void testGetSnapshotWithInvalidReference() {
    PeerRepositoryImpl peerRepository = new PeerRepositoryImpl();
    String ref = "not at all a valid reference";
    peerRepository.getSnapshot(testContext, ref);
  }
}
