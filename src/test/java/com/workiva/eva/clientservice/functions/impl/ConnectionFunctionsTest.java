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
import eva.Util;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.EnvironmentService;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.edn.Transactions;
import com.workiva.eva.clientservice.peer.TestPeerRepository;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.reference.SnapshotReference;

import java.util.List;

@SpringBootTest
public class ConnectionFunctionsTest {

  static TestPeerRepository peerRepository = new TestPeerRepository();
  static EnvironmentService environmentService =
      Mockito.spy(new TestEnvironmentService(true, true, 1));
  static RequestContext testContext = new RequestContext.Builder().build();

  @Test
  public void latestTTest() {
    ConnectionReference ref = new ConnectionReference("test-tenant", "test-category", "test-label");
    Object result = ConnectionFunctions.latestT(peerRepository, ref, testContext);
    Assert.assertTrue(result instanceof Long);
    Assert.assertEquals(0L, result);
  }

  @Test
  public void latestTWithTxTest() {
    ConnectionReference ref =
        new ConnectionReference("test-tenant-tx", "test-category-tx", "test-label-tx");
    Object result = ConnectionFunctions.latestT(peerRepository, ref, testContext);
    Assert.assertTrue(result instanceof Long);
    Assert.assertEquals(0L, result);
    Connection conn = peerRepository.getConnection(testContext, ref);
    conn.transact((List) Util.read(Transactions.defaultSchema));
    result = ConnectionFunctions.latestT(peerRepository, ref, testContext);
    Assert.assertTrue(result instanceof Long);
    Assert.assertEquals(1L, result);
  }

  @Test
  public void latestTWithAsOf() {
    ConnectionReference ref =
        new ConnectionReference(
            "test-tenant-tx-asof", "test-category-tx-asof", "test-label-tx-asof");
    Object result = ConnectionFunctions.latestT(peerRepository, ref, testContext);
    Assert.assertTrue(result instanceof Long);
    Assert.assertEquals(0L, result);
    Connection conn = peerRepository.getConnection(testContext, ref);
    conn.transact((List) Util.read(Transactions.defaultSchema));
    SnapshotReference dbRef =
        new SnapshotReference(
            "test-tenant-tx-asof", "test-category-tx-asof", "test-label-tx-asof", 0L, false);
    result = ConnectionFunctions.latestT(peerRepository, dbRef, testContext);
    Assert.assertTrue(result instanceof Long);
    Assert.assertEquals(1L, result);
  }
}
