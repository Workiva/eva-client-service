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

package com.workiva.eva.clientservice.controllers.v1;

import clojure.lang.Keyword;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentVector;
import eva.Connection;
import eva.Database;
import eva.Peer;
import eva.Util;
import eva.error.v1.EvaErrorCode;
import eva.error.v1.EvaException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.edn.Transactions;
import com.workiva.eva.clientservice.peer.TestPeerRepository;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.serialization.SerializerUtils;
import com.workiva.eva.clientservice.controllers.QueryParameters;
import org.springframework.web.bind.annotation.ValueConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.workiva.eva.clientservice.TestUtils.CATEGORY;
import static com.workiva.eva.clientservice.TestUtils.CONNECTION_REF;
import static com.workiva.eva.clientservice.TestUtils.CONNECTION_REF_STRING;
import static com.workiva.eva.clientservice.TestUtils.SNAP_REF_STRING;
import static com.workiva.eva.clientservice.TestUtils.TENANT;
import static com.workiva.eva.clientservice.TestUtils.LABEL;

public class PeerControllerV1Test {

  static TestPeerRepository peerRepository = new TestPeerRepository();
  static TestEnvironmentService environmentService = new TestEnvironmentService(true, true, 1);
  static final RequestContext testContext = new RequestContext.Builder().build();

  static final Map<String, String> HEADERS =
      new HashMap<String, String>() {
        {
          put(SerializerUtils.ACCEPT_HEADER, SerializerUtils.DEFAULT_MIME_TYPE);
        }
      };

  static PeerControllerV1 controller = new PeerControllerV1(peerRepository, environmentService);

  @Before
  public void setUp() {
    ConnectionReference ref = new ConnectionReference(TENANT, CATEGORY, LABEL);
    Connection conn = peerRepository.getConnection(testContext, ref);
    conn.transact((List) Util.read(Transactions.defaultSchema));
  }

  @Test
  public void testCheckDefaultSchema() throws Throwable {
    // TODO: Possibly should have another constructor to take in the String map
    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("p[0]", SNAP_REF_STRING);
          }
        };
    QueryParameters params = new QueryParameters();
    params.setP(rawParams);
    final String queryString = "[:find ?attr :in $ :where [_ :db/ident ?attr]]";
    String controllerQueryResult =
        controller.query("correlation-id", HEADERS, TENANT, CATEGORY, queryString, params);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object[] dbArray = {conn.db()};
    PersistentVector queryResult = Peer.query(Util.read(queryString), dbArray);
    String queryResultString = SerializerUtils.serialize(HEADERS, queryResult);
    Assert.assertEquals(queryResultString, controllerQueryResult);
  }

  @Test
  public void transactTest() throws Throwable {
    String controllerTxResult =
        controller.transact(
            "correlation-id",
            HEADERS,
            TENANT,
            CATEGORY,
            CONNECTION_REF_STRING,
            Transactions.singleBook);

    // Check that db-before/after have been formatted to client-service references
    Assert.assertTrue(controllerTxResult.contains("#eva.client.service/snapshot-ref"));

    // Check that book was added to database
    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Database db = conn.db();
    Object[] dbArray = {db, "First Book"};
    PersistentVector queryResult =
        Peer.query(Util.read("[:find ?b :in $ ?t :where [?b :book/title ?t]]"), dbArray);

    Assert.assertTrue(queryResult.length() > 0);
  }

  @Test
  public void withTest() throws Throwable {
    String controllerTxResult =
        controller.with(
            "correlation-id",
            HEADERS,
            TENANT,
            CATEGORY,
            CONNECTION_REF_STRING,
            Transactions.singleBook);

    // Check that keys returned from a regular tx are returned for a `with` call as well
    Assert.assertTrue(controllerTxResult.contains(":tempids"));
    Assert.assertTrue(controllerTxResult.contains(":db-before"));
    Assert.assertTrue(controllerTxResult.contains(":db-after"));
    Assert.assertTrue(controllerTxResult.contains(":tx-data"));

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Database db = conn.db();
    Object[] dbArray = {db, "First Book"};
    PersistentVector queryResult =
        Peer.query(Util.read("[:find ?b :in $ ?t :where [?b :book/title ?t]]"), dbArray);

    // Querying for this value should return nothing, as `with` does not durably persist anything.
    Assert.assertTrue(queryResult.length() == 0);
  }

  @Test
  public void queryTest() throws Throwable {
    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("p[0]", SNAP_REF_STRING);
          }
        };

    QueryParameters params = new QueryParameters();
    params.setP(rawParams);
    final String query = "[:find ?attr :in $ :where [_ :db/ident ?attr]]";
    String controllerQueryResult =
        controller.query("correlation-id", HEADERS, TENANT, CATEGORY, query, params);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object[] dbArray = {conn.db()};

    PersistentVector queryResult = Peer.query(query, dbArray);
    String queryResultString = SerializerUtils.serialize(HEADERS, queryResult);

    Assert.assertEquals(queryResultString, controllerQueryResult);
  }

  @Test(expected = EvaException.class)
  public void testInvalidQuery() throws Throwable {
    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("p[0]", SNAP_REF_STRING);
          }
        };

    QueryParameters params = new QueryParameters();
    params.setP(rawParams);
    final String query = "[:fin ?attr :in $ :where [_ :db/doesnt-exist ?attr]]";
    String controllerQueryResult = controller.query(null, HEADERS, TENANT, CATEGORY, query, params);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object[] dbArray = {conn.db()};

    PersistentVector queryResult = Peer.query(query, dbArray);
    String queryResultString = SerializerUtils.serialize(HEADERS, queryResult);

    Assert.assertEquals(queryResultString, controllerQueryResult);
  }

  @Test
  public void testQueryInvalidAttribute() throws Throwable {
    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("p[0]", SNAP_REF_STRING);
          }
        };

    QueryParameters params = new QueryParameters();
    params.setP(rawParams);
    final String query = "[:find ?attr :in $ :where [_ :db/doesnt-exist ?attr]]";
    String controllerQueryResult = controller.query(null, HEADERS, TENANT, CATEGORY, query, params);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object[] dbArray = {conn.db()};

    PersistentVector queryResult = Peer.query(query, dbArray);
    String queryResultString = SerializerUtils.serialize(HEADERS, queryResult);

    Assert.assertEquals(queryResultString, controllerQueryResult);
  }

  @Test
  public void pullTest() throws Throwable {
    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);

    conn.transact(
        (List)
            Util.read(
                "[[:db/add #db/id [:db.part/user] :book/title \"Pull Test Book\"]"
                    + "[:db/add #db/id [:db.part/tx] :author/name \"Pull Test Author\"]]"));

    Object[] dbArray = {conn.db()};
    PersistentVector queryResult =
        Peer.query(
            Util.read("[:find ?b :in $ :where [?b :book/title \"Pull Test Book\"]]"), dbArray);

    long entityId = (long) ((PersistentVector) queryResult.get(0)).get(0);

    QueryParameters params = new QueryParameters();
    String result =
        controller.pull(
            ValueConstants.DEFAULT_NONE,
            HEADERS,
            TENANT,
            CATEGORY,
            SNAP_REF_STRING,
            Long.toString(entityId),
            "[*]",
            params);

    Object pullResult = conn.db().pull("[*]", entityId);
    String pullResultString = SerializerUtils.serialize(HEADERS, pullResult);
    Assert.assertEquals(pullResultString, result);
  }

  @Test
  public void pullManyTest() throws Throwable {
    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);

    conn.transact(
        (List)
            Util.read(
                "[[:db/add #db/id [:db.part/user] :book/title \"Pull Many Test Book 1\"]"
                    + "[:db/add #db/id [:db.part/tx] :author/name \"Pull Many Test Author 1\"]]"));
    conn.transact(
        (List)
            Util.read(
                "[[:db/add #db/id [:db.part/user] :book/title \"Pull Many Test Book 2\"]"
                    + "[:db/add #db/id [:db.part/tx] :author/name \"Pull Many Test Author 2\"]]"));

    Object[] dbArray = {conn.db()};
    PersistentVector queryResult =
        Peer.query(
            Util.read(
                "[:find ?b :in $ :where [?b :book/title ?title]"
                    + "[(clojure.string/starts-with? ?title \"Pull Many Test Book\")]]"),
            dbArray);

    long firstEntityId = (long) ((PersistentVector) queryResult.get(0)).get(0);
    long secondEntityId = (long) ((PersistentVector) queryResult.get(1)).get(0);

    QueryParameters params = new QueryParameters();
    String controllerResult =
        controller.pull(
            "correlation-id",
            HEADERS,
            TENANT,
            CATEGORY,
            SNAP_REF_STRING,
            String.format("[%d %d]", firstEntityId, secondEntityId),
            "[*]",
            params);

    Object entityIds =
        SerializerUtils.deserialize(
            HEADERS, String.format("[%d %d]", firstEntityId, secondEntityId));
    Object pullResult = conn.db().pullMany("[*]", (List) entityIds);
    String pullResultString = SerializerUtils.serialize(HEADERS, pullResult);
    Assert.assertEquals(controllerResult, pullResultString);
  }

  // TODO: this doesnt actually perform the CAS without it being transacted, do that in a separate
  // test
  // TODO: should have a custom transaction function somewhere
  @Test
  public void successfulInvokeTest() throws Throwable {
    String dbDoc = "db/doc";
    String oldValue = "The default database partition.";
    String newValue = "Testing";

    // TODO this fails in invoke if keyed with p[x], yet query is fine? Investigate
    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("0", SNAP_REF_STRING);
            put("1", "0");
            put("2", String.format(":%s", dbDoc));
            put("3", String.format("\"%s\"", oldValue));
            put("4", String.format("\"%s\"", newValue));
          }
        };

    String function = ":db.fn/cas";
    QueryParameters params = new QueryParameters();
    params.setP(rawParams);
    String controllerResults =
        controller.invoke(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, function, params);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result =
        conn.db()
            .invoke(
                Keyword.intern("db.fn/cas"),
                conn.db(),
                0,
                Keyword.intern(dbDoc),
                oldValue,
                newValue);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(controllerResults, resultString);
  }

  @Test
  public void entidTest() throws Throwable {
    String controllerResults =
        controller.entid("correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, "1", false);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().entid(1);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void identTest() throws Throwable {
    String controllerResults =
        controller.ident(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/ident", false);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().ident(Keyword.intern("db", "ident"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void entidNilTest() throws Throwable {
    String controllerResults =
        controller.entid("correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, "-1", false);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().entid(-1);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void identNilTest() throws Throwable {
    String controllerResults =
        controller.ident(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/nope", false);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().ident(Keyword.intern("db", "nope"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void entidStrictTest() throws Throwable {
    String controllerResults =
        controller.entid("correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, "1", true);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().entidStrict(1);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void identStrictTest() throws Throwable {
    String controllerResults =
        controller.ident(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/ident", true);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().identStrict(Keyword.intern("db", "ident"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void entidStrictNilTest() throws Throwable {
    String controllerResults =
        controller.entid("correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, "-1", true);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().entidStrict(-1);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void identStrictNilTest() throws Throwable {
    String controllerResults =
        controller.ident(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/nope", true);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().identStrict(Keyword.intern("db", "nope"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void datomsTest() throws Throwable {
    String controllerResults =
        controller.datoms(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":eavt", "0");

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().datoms(Keyword.intern("eavt"), 0L);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void datomsListTest() throws Throwable {
    String controllerResults =
        controller.datoms(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":eavt", "[0]");

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().datoms(Keyword.intern("eavt"), 0L);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void datomsListMultipleTest() throws Throwable {
    String controllerResults =
        controller.datoms(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":eavt", "[0 3]");

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().datoms(Keyword.intern("eavt"), 0L, 3L);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void datomsListNoneTest() throws Throwable {
    String controllerResults =
        controller.datoms(
            "correlation-id",
            HEADERS,
            TENANT,
            CATEGORY,
            SNAP_REF_STRING,
            ":eavt",
            "[0 3 :db/none]");

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().datoms(Keyword.intern("eavt"), 0L, 3L, Keyword.intern(":db/none"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void extantEntityTest() throws Throwable {
    String controllerResults =
        controller.extantEntity(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/doc");

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().isExtantEntity(Keyword.intern("db", "doc"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void extantEntityNilTest() throws Throwable {
    String controllerResults =
        controller.extantEntity(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/nope");

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().isExtantEntity(Keyword.intern("db", "nope"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void attributeTest() throws Throwable {
    String controllerResults =
        controller.attribute(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/doc", false);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().attribute(Keyword.intern("db", "doc"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void attributeStrictTest() throws Throwable {
    String controllerResults =
        controller.attribute(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/doc", true);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().attribute(Keyword.intern("db", "doc"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void attributeNilTest() throws Throwable {
    String controllerResults =
        controller.attribute(
            "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/nope", false);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.db().attribute(Keyword.intern("db", "nope"));
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(resultString, controllerResults);
  }

  @Test
  public void attributeNilStrictTest() throws Throwable {
    try {
      String controllerResults =
          controller.attribute(
              "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, ":db/nope", true);
      Assert.fail("Expected exception");
    } catch (EvaException e) {
      Assert.assertEquals(EvaErrorCode.COERCION_FAILURE, e.getErrorCode());
    }
  }

  @Test
  public void logOnlyStartTest() throws Throwable {
    String controllerResults =
        controller.txRange("correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, 0L, -1L);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.log().txRange(0, conn.latestT());
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(controllerResults, resultString);
  }

  @Test
  public void txRangeExplicitEndTest() throws Throwable {
    String controllerResults =
        controller.txRange("correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, 0L, 1L);

    Connection conn = peerRepository.getConnection(testContext, CONNECTION_REF);
    Object result = conn.log().txRange(0, 1);
    String resultString = SerializerUtils.serialize(HEADERS, result);

    Assert.assertEquals(controllerResults, resultString);
  }

  @Test
  public void txRangeOutOfBoundsTest() throws Throwable {
    try {
      String controllerResults =
          controller.txRange(
              "correlation-id", HEADERS, TENANT, CATEGORY, SNAP_REF_STRING, 100000L, -1L);
      Assert.fail("Expected EvaException, start-t must be less than end-t");
    } catch (EvaException e) {
      // nothing
    }
  }

  @Test
  public void getStatusTest() throws Throwable {
    String results = controller.status("correlation-id", HEADERS, TENANT, CATEGORY, LABEL);
    PersistentArrayMap resultMap = SerializerUtils.deserialize(HEADERS, results);

    long latestT = (long) resultMap.get(Keyword.intern("latestT"));

    Assert.assertTrue(latestT >= 0);
  }
}
