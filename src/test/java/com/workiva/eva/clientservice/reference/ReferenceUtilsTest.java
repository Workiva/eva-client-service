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
import org.junit.Assert;
import org.junit.Test;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.exceptions.ClientServiceException;
import com.workiva.eva.clientservice.exceptions.FunctionInvokeException;
import com.workiva.eva.clientservice.exceptions.ReferenceException;
import com.workiva.eva.clientservice.peer.TestPeerRepository;
import com.workiva.eva.clientservice.serialization.SerializerUtils;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.fail;

public class ReferenceUtilsTest {
  private static TestPeerRepository peerRepository = new TestPeerRepository();
  static TestEnvironmentService environmentService = new TestEnvironmentService(true, true, 1);

  private static final String CATEGORY = "category";
  private static final String TENANT = "tenant";
  private static final String LABEL = "label";

  private static final Map<String, String> headers =
      new HashMap<String, String>() {
        {
          put(SerializerUtils.ACCEPT_HEADER, SerializerUtils.DEFAULT_MIME_TYPE);
        }
      };

  private static final String VALID_SNAPSHOT_MAP = "#eva.client.service/snapshot-ref { :label %s }";
  private static final String VALID_SNAPSHOT_ASOF_MAP =
      "#eva.client.service/snapshot-ref { :label %s :as-of %d }";
  private static final String VALID_SNAPSHOT_SYNC_DB_MAP =
      "#eva.client.service/snapshot-ref { :label %s :sync-db %s }";

  static final RequestContext testContext =
      new RequestContext.Builder().httpHeaders(headers).build();

  @Test(expected = ReferenceException.class)
  public void testVerifyLabelThrowsExceptionIfNull() {
    ReferenceUtils.deserializeEdn(
        testContext,
        peerRepository,
        TENANT,
        CATEGORY,
        "#eva.client.service/snapshot-ref [ \"anything\" ]");
  }

  @Test(expected = ReferenceException.class)
  public void testCreateSnapshotReferenceMapWithEmptyLabel() {
    String refString = String.format(VALID_SNAPSHOT_MAP, "\"\"");
    ReferenceUtils.deserializeEdn(testContext, peerRepository, TENANT, CATEGORY, refString);
  }

  @Test
  public void testCreateSnapshotReferenceMapWithSet() {
    String refString = String.format(VALID_SNAPSHOT_MAP, String.format("\"%s\"", LABEL));
    Object ref =
        ReferenceUtils.deserializeEdn(testContext, peerRepository, TENANT, CATEGORY, refString);
    Assert.assertEquals(SnapshotReference.class, ref.getClass());
  }

  @Test
  public void testCreateSnapshotReferenceWithMap() {
    String refString = String.format(VALID_SNAPSHOT_ASOF_MAP, String.format("\"%s\"", LABEL), 123L);
    Object ref =
        ReferenceUtils.deserializeEdn(testContext, peerRepository, TENANT, CATEGORY, refString);
    Assert.assertEquals(SnapshotReference.class, ref.getClass());

    SnapshotReference snapRef = (SnapshotReference) ref;
    Assert.assertEquals(LABEL, snapRef.label());
    Assert.assertEquals(CATEGORY, snapRef.category());
    Assert.assertEquals(TENANT, snapRef.tenant());
    Assert.assertEquals(123, snapRef.asOf().longValue());
  }

  @Test
  public void testCreateSnapshotReferenceWithTrueSyncDbMap() {
    String refString =
        String.format(VALID_SNAPSHOT_SYNC_DB_MAP, String.format("\"%s\"", LABEL), true);
    SnapshotReference ref =
        (SnapshotReference)
            ReferenceUtils.deserializeEdn(testContext, peerRepository, TENANT, CATEGORY, refString);
    Assert.assertEquals(LABEL, ref.label());

    Assert.assertEquals(CATEGORY, ref.category());
    Assert.assertEquals(TENANT, ref.tenant());
    Assert.assertTrue(ref.syncDb());
  }

  @Test
  public void testCreateSnapshotReferenceWithFalseSyncDbMap() {
    String refString =
        String.format(VALID_SNAPSHOT_SYNC_DB_MAP, String.format("\"%s\"", LABEL), false);
    SnapshotReference ref =
        (SnapshotReference)
            ReferenceUtils.deserializeEdn(testContext, peerRepository, TENANT, CATEGORY, refString);

    Assert.assertEquals(LABEL, ref.label());
    Assert.assertEquals(CATEGORY, ref.category());
    Assert.assertEquals(TENANT, ref.tenant());
    Assert.assertFalse(ref.syncDb());
  }

  @Test
  public void testCreateDatabaseWithSyncDb() {
    SnapshotReference snapshotRef = new SnapshotReference(TENANT, CATEGORY, LABEL, null, true);
    Connection mockConnection = Mockito.mock(Connection.class);
    try {
      snapshotRef.getSnapshot(testContext, mockConnection);
    } catch (NullPointerException e) {
    }
    Mockito.verify(mockConnection, Mockito.times(1)).syncDb();
  }

  @Test
  public void testCreateSnapshotReferenceFromConnectionReference() {
    ConnectionReference connRef = new ConnectionReference(TENANT, CATEGORY, LABEL);
    SnapshotReference snapshotRef = new SnapshotReference(connRef, 123L, false);

    Assert.assertEquals(snapshotRef.tenant(), connRef.tenant());
    Assert.assertEquals(snapshotRef.category(), connRef.category());
    Assert.assertEquals(snapshotRef.label(), connRef.label());
    Assert.assertEquals(snapshotRef.asOf(), Long.valueOf(123));
  }

  @Test
  public void testSnapshotReferenceToMapString() {
    SnapshotReference snapshotRef = new SnapshotReference(TENANT, CATEGORY, LABEL, 123L, false);

    Assert.assertEquals(TENANT, snapshotRef.tenant());
    Assert.assertEquals(CATEGORY, snapshotRef.category());
    Assert.assertEquals(LABEL, snapshotRef.label());
    Assert.assertEquals(123L, snapshotRef.asOf().longValue());
    Assert.assertFalse(snapshotRef.syncDb());
  }

  @Test
  public void testReplaceDBwithSnapshotMapRef() {
    ConnectionReference connRef = new ConnectionReference(TENANT, CATEGORY, LABEL);
    Database db = peerRepository.getSnapshot(testContext, connRef);
    Connection conn = peerRepository.getConnection(testContext, connRef);
    Map<Object, Object> map =
        new HashMap<Object, Object>() {
          {
            put(Keyword.intern("db-before"), db);
            put(Keyword.intern("db-after"), db);
          }
        };

    ReferenceUtils.replaceDbWithSnapshotReference(testContext, map, connRef);

    SnapshotReference dbBefore = (SnapshotReference) map.get(Keyword.intern("db-before"));
    Assert.assertEquals(dbBefore.tenant(), TENANT);
    Assert.assertEquals(dbBefore.category(), CATEGORY);
    Assert.assertEquals(dbBefore.label(), LABEL);
    Assert.assertEquals(dbBefore.asOf(), conn.latestT());

    SnapshotReference dbAfter = (SnapshotReference) map.get(Keyword.intern("db-after"));
    Assert.assertEquals(dbAfter.tenant(), TENANT);
    Assert.assertEquals(dbAfter.category(), CATEGORY);
    Assert.assertEquals(dbAfter.label(), LABEL);
    Assert.assertEquals(dbAfter.asOf(), conn.latestT());
  }

  private static final String SNAPSHOT_INLINE_ASOF_MAP =
      "#eva.client.service/snapshot-ref { :label \"%s\" :as-of #eva.client.service/inline %s }";

  @Test
  public void testInitializeInlineFunctions() {
    String edn =
        String.format(
            SNAPSHOT_INLINE_ASOF_MAP,
            LABEL,
            String.format(
                "{ :fn latestT :params [#eva.client.service/snapshot-ref { :label \"%s\" }]}",
                LABEL));

    Object ref = ReferenceUtils.deserializeEdn(testContext, peerRepository, TENANT, CATEGORY, edn);
    Assert.assertEquals(SnapshotReference.class, ref.getClass());

    SnapshotReference snapRef = (SnapshotReference) ref;
    Assert.assertTrue(snapRef.asOf() >= 0);
  }

  @Test(expected = FunctionInvokeException.class)
  public void testInitializeLatestTVector() {
    ReferenceUtils.deserializeEdn(
        testContext,
        peerRepository,
        TENANT,
        CATEGORY,
        String.format(
            SNAPSHOT_INLINE_ASOF_MAP,
            String.format("\"%s\"", LABEL),
            String.format(
                "[ latestT [#eva.client.service/snapshot-ref { :label \"%s\" }] ]", LABEL)));
  }

  @Test(expected = ClientServiceException.class)
  public void testInitializeLatestTFailure() {
    ReferenceUtils.deserializeEdn(
        testContext,
        peerRepository,
        TENANT,
        CATEGORY,
        String.format(
            SNAPSHOT_INLINE_ASOF_MAP,
            String.format("\"%s\"", LABEL),
            String.format(
                "[ latestT [#eva.client.service/snapshot-ref { :label \"%s\" } 123 ] ]", LABEL)));
  }

  @Test(expected = FunctionInvokeException.class)
  public void testInlineFunctionsInvalidVectorTooFew() {
    ReferenceUtils.deserializeEdn(
        testContext,
        peerRepository,
        TENANT,
        CATEGORY,
        String.format(
            SNAPSHOT_INLINE_ASOF_MAP,
            String.format("\"%s\"", LABEL),
            String.format("[ [#eva.client.service/snapshot-ref { :label \"%s\" }] ]", LABEL)));
  }

  @Test(expected = FunctionInvokeException.class)
  public void testInlineFunctionsInvalidVectorTooMany() {
    ReferenceUtils.deserializeEdn(
        testContext,
        peerRepository,
        TENANT,
        CATEGORY,
        String.format(
            SNAPSHOT_INLINE_ASOF_MAP,
            String.format("\"%s\"", LABEL),
            String.format(
                "[ latestT extra-parameter [#eva.client.service/snapshot-ref { :label \"%s\" }] ]",
                LABEL)));
  }

  @Test
  public void testInlineFunctionsInvalidEDN() {
    try {
      ReferenceUtils.deserializeEdn(
          testContext,
          peerRepository,
          TENANT,
          CATEGORY,
          String.format(SNAPSHOT_INLINE_ASOF_MAP, String.format("\"%s\"", LABEL), "#{}"));
      fail("Expected FunctionInvokeException");
    } catch (FunctionInvokeException e) {
      Assert.assertTrue(e.getMessage().contains("Unsupported snapshot reference type."));
    }
  }

  @Test
  public void testInlineFunctionsNoFunction() {
    try {
      ReferenceUtils.deserializeEdn(
          testContext,
          peerRepository,
          TENANT,
          CATEGORY,
          String.format(
              SNAPSHOT_INLINE_ASOF_MAP,
              String.format("\"%s\"", LABEL),
              String.format(
                  "{ :fn nil :params [#eva.client.service/snapshot-ref { :label \"%s\" }]}",
                  LABEL)));
      fail("Expected FunctionInvokeException");
    } catch (FunctionInvokeException e) {
      Assert.assertTrue(e.getMessage().contains("fn parameter is nil"));
    }
  }

  @Test
  public void testInlineFunctionsFunctionNonSymbol() {
    try {
      ReferenceUtils.deserializeEdn(
          testContext,
          peerRepository,
          TENANT,
          CATEGORY,
          String.format(
              SNAPSHOT_INLINE_ASOF_MAP,
              String.format("\"%s\"", LABEL),
              String.format(
                  "{ :fn \"failure\" :params [#eva.client.service/snapshot-ref { :label \"%s\" }]}",
                  LABEL)));
      fail("Expected FunctionInvokeException");
    } catch (FunctionInvokeException e) {
      Assert.assertTrue(e.getMessage().contains("fn parameter is not a symbol"));
    }
  }

  @Test
  public void testInlineFunctionsParamsNil() {
    try {
      ReferenceUtils.deserializeEdn(
          testContext,
          peerRepository,
          TENANT,
          CATEGORY,
          String.format(
              SNAPSHOT_INLINE_ASOF_MAP,
              String.format("\"%s\"", LABEL),
              "{ :fn latestT :params nil }}"));
      fail("Expected FunctionInvokeException");
    } catch (FunctionInvokeException e) {
      Assert.assertTrue(e.getMessage().contains("params parameter is nil"));
    }
  }

  @Test
  public void testInlineFunctionsParamsNotVector() {
    try {
      ReferenceUtils.deserializeEdn(
          testContext,
          peerRepository,
          TENANT,
          CATEGORY,
          String.format(
              SNAPSHOT_INLINE_ASOF_MAP,
              String.format("\"%s\"", LABEL),
              "{ :fn latestT :params {:fail \"yup\" } }}"));
      fail("Expected FunctionInvokeException");
    } catch (FunctionInvokeException e) {
      Assert.assertTrue(e.getMessage().contains("params parameter is not a list"));
    }
  }

  @Test
  public void testInlineFunctionsNilFunction() {
    try {
      ReferenceUtils.deserializeEdn(
          testContext,
          peerRepository,
          TENANT,
          CATEGORY,
          String.format(SNAPSHOT_INLINE_ASOF_MAP, String.format("\"%s\"", LABEL), "nil"));
      fail("Expected FunctionInvokeException");
    } catch (FunctionInvokeException e) {
      Assert.assertTrue(e.getMessage().contains("nil inline function parameter"));
    }
  }

  @Test(expected = FunctionInvokeException.class)
  public void testInlineFunctionsUnknownFunction() {
    ReferenceUtils.deserializeEdn(
        testContext,
        peerRepository,
        TENANT,
        CATEGORY,
        String.format(
            SNAPSHOT_INLINE_ASOF_MAP,
            String.format("\"%s\"", LABEL),
            String.format(
                "[ unknown-function [#eva.client.service/snapshot-ref { :label \"%s\" }] ]",
                LABEL)));
  }

  @Test
  public void testSnapshotEdnToSnapshot() {
    SnapshotReference ref = new SnapshotReference(TENANT, CATEGORY, LABEL, 1L, false);
    String refEdn = SerializerUtils.serialize(new HashMap<>(), ref);
    Assert.assertEquals(ref.toString(), refEdn);
  }
}
