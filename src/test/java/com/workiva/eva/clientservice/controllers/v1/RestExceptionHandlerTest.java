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

import clojure.lang.ExceptionInfo;
import clojure.lang.Keyword;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashSet;
import clojure.lang.PersistentVector;
import eva.Util;
import eva.error.v1.EvaErrorCode;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.workiva.eva.clientservice.config.EnvironmentService;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.controllers.RestExceptionHandler;
import com.workiva.eva.clientservice.edn.Transactions;
import com.workiva.eva.clientservice.peer.TestPeerRepository;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.workiva.eva.clientservice.TestUtils.CATEGORY;
import static com.workiva.eva.clientservice.TestUtils.CONNECTION_REF_STRING;
import static com.workiva.eva.clientservice.TestUtils.LABEL;
import static com.workiva.eva.clientservice.TestUtils.TENANT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class RestExceptionHandlerTest {

  // TODO - eventually we may want to go back and change all our controller unittests to use MockMVC

  private MockMvc mockMvc;
  static TestPeerRepository peerRepository = new TestPeerRepository();
  static EnvironmentService environmentService =
      Mockito.spy(new TestEnvironmentService(true, true, 1));

  static PeerControllerV1 controller = new PeerControllerV1(peerRepository, environmentService);

  static final Map<String, String> HEADERS =
      new HashMap<String, String>() {
        {
          put(SerializerUtils.ACCEPT_HEADER, SerializerUtils.DEFAULT_MIME_TYPE);
        }
      };

  Keyword MESSAGE = Keyword.intern("message");
  Keyword INFO = Keyword.intern("ex-info");
  Keyword DATA = Keyword.intern("ex-data");
  Keyword EXPLANATION = Keyword.intern("explanation");
  Keyword EVA_ERROR = Keyword.intern("eva/error");
  Keyword EVA_ERROR_CODE = Keyword.intern("eva.error/code");
  Keyword FN = Keyword.intern("fn");
  Keyword CAUSE = Keyword.intern("cause");
  Keyword CODE = Keyword.intern("code");
  Keyword EX_TYPE = Keyword.intern("type");

  @Before
  public void setup() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PeerControllerV1(peerRepository, environmentService))
            .setControllerAdvice(new RestExceptionHandler(environmentService))
            .build();
  }

  @Test
  public void testInvalidQuerySyntax() throws Exception {
    Mockito.when(environmentService.sanitizeExceptions()).thenReturn(false);
    invalidQuerySyntaxHelper(false);
  }

  private void invalidQuerySyntaxHelper(boolean sanitized) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/q/{tenant}/{category}", "test", "test")
                    .header("_cid", "test")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "query", "[:fin ?attr :in $ :where [_ :db/identt ?attr]]"),
                                    new BasicNameValuePair(
                                        "p[0]",
                                        "#eva.client.service/snapshot-ref { :label \"test\" }"))))))
            .andExpect(status().is((int) EvaErrorCode.INCORRECT_QUERY_SYNTAX.getHttpErrorCode()))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);
    if (!sanitized) {
      Assert.assertTrue(ednContent.get(MESSAGE).toString().contains("Invalid query form"));
    } else {
      Assert.assertEquals("", ednContent.get(MESSAGE));
    }
    PersistentArrayMap ednExInfo = (PersistentArrayMap) Util.read(ednContent.get(INFO).toString());
    Assert.assertEquals("Malformed query or rules.", ednExInfo.get(EXPLANATION));
    Assert.assertEquals("IncorrectQuerySyntax", ednExInfo.get(EX_TYPE));
    Assert.assertEquals(
        (int) EvaErrorCode.INCORRECT_QUERY_SYNTAX.getCode(),
        Math.toIntExact((Long) ednExInfo.get(CODE)));
    if (!sanitized) {
      PersistentArrayMap ednExData =
          (PersistentArrayMap) Util.read(ednContent.get(DATA).toString());
      Assert.assertEquals(
          Keyword.intern("query.translation/invalid-form"), ednExData.get(EVA_ERROR));
    } else {
      Assert.assertEquals(PersistentArrayMap.create(new HashMap()), ednContent.get(DATA));
    }
  }

  @Test
  public void testUnknownIdent() throws Exception {
    Mockito.when(environmentService.sanitizeExceptions()).thenReturn(false);
    unknownIdentHelper(false);
  }

  private void unknownIdentHelper(boolean sanitized) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/q/{tenant}/{category}", "test", "test")
                    .header("_cid", "test")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "query", "[:find ?attr :in $ :where [_ :db/identt ?attr]]"),
                                    new BasicNameValuePair(
                                        "p[0]",
                                        "#eva.client.service/snapshot-ref { :label \"test\" }"))))))
            .andExpect(status().is(200))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentVector ednContent = (PersistentVector) Util.read(content);
    Assert.assertTrue(ednContent.length() == 0);
  }

  @Test
  public void testTransactionCASError() throws Exception {
    Mockito.when(environmentService.sanitizeExceptions()).thenReturn(false);
    transactionCASErrorHelper(false);
  }

  private void transactionCASErrorHelper(boolean sanitized) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/transact/{tenant}/{category}", "test", "test")
                    .header("_cid", "test")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "transaction",
                                        "[[:db.fn/cas (eva/q get-entity-id db \"Jeff Bridges\") :account/balance 100 200]]"),
                                    new BasicNameValuePair(
                                        "reference",
                                        "#eva.client.service/connection-ref { :label \"test\" }"))))))
            .andExpect(
                status().is((int) EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getHttpErrorCode()))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);
    Assert.assertEquals(
        sanitized ? "" : "A tx-fn threw an exception: aborting transaction.",
        ednContent.get(MESSAGE));
    PersistentArrayMap ednExInfo = (PersistentArrayMap) Util.read(ednContent.get(INFO).toString());
    Assert.assertEquals("A transaction function threw an exception.", ednExInfo.get(EXPLANATION));
    Assert.assertEquals("TransactionFunctionException", ednExInfo.get(EX_TYPE));
    Assert.assertEquals(
        EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
        Math.toIntExact((Long) ednExInfo.get(CODE)));
    if (!sanitized) {
      PersistentArrayMap ednExData =
          (PersistentArrayMap) Util.read(ednContent.get(DATA).toString());
      Assert.assertEquals("db.fn/cas", ednExData.get(FN));
      Assert.assertEquals(
          Keyword.intern("transaction-pipeline/tx-fn-threw"), ednExData.get(EVA_ERROR));
      Assert.assertEquals(
          EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
          Math.toIntExact((Long) ednExData.get(EVA_ERROR_CODE)));
    } else {
      Assert.assertEquals(PersistentArrayMap.create(new HashMap()), ednContent.get(DATA));
    }
    PersistentArrayMap causeContent = (PersistentArrayMap) ednContent.get(CAUSE);
    Assert.assertEquals(sanitized ? "" : null, causeContent.get(MESSAGE));
    PersistentArrayMap causeInfo = (PersistentArrayMap) causeContent.get(INFO);
    Assert.assertEquals(NullPointerException.class.getName(), causeInfo.get(EX_TYPE));
  }

  @Test
  public void testTxFunctionThrowsIExceptionInfo() throws Throwable {
    Mockito.when(environmentService.sanitizeExceptions()).thenReturn(false);
    txFunctionThrowsIExceptionInfoHelper(false);
  }

  public void txFunctionThrowsIExceptionInfoHelper(boolean sanitized) throws Throwable {
    controller.transact(
        "correlation-id",
        HEADERS,
        TENANT,
        CATEGORY,
        CONNECTION_REF_STRING,
        Transactions.exInfoTxFunction);
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/transact/{tenant}/{category}", TENANT, CATEGORY)
                    .header("_cid", LABEL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "transaction", Transactions.callexInfoTxFunction),
                                    new BasicNameValuePair("reference", CONNECTION_REF_STRING))))))
            .andExpect(
                status().is((int) EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getHttpErrorCode()))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);
    Assert.assertEquals(
        sanitized ? "" : "A tx-fn threw an exception: aborting transaction.",
        ednContent.get(MESSAGE));
    PersistentArrayMap ednExInfo = (PersistentArrayMap) ednContent.get(INFO);
    Assert.assertEquals("A transaction function threw an exception.", ednExInfo.get(EXPLANATION));
    Assert.assertEquals("TransactionFunctionException", ednExInfo.get(EX_TYPE));
    Assert.assertEquals(
        EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
        Math.toIntExact((Long) ednExInfo.get(CODE)));
    if (!sanitized) {
      PersistentArrayMap ednExData = (PersistentArrayMap) ednContent.get(DATA);
      Assert.assertEquals("throw-ex-info", ednExData.get(FN));
      Assert.assertEquals(
          Keyword.intern("transaction-pipeline/tx-fn-threw"), ednExData.get(EVA_ERROR));
      Assert.assertEquals(
          EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
          Math.toIntExact((Long) ednExData.get(EVA_ERROR_CODE)));
    } else {
      Assert.assertEquals(PersistentArrayMap.create(new HashMap()), ednContent.get(DATA));
    }
    PersistentArrayMap causeContent = (PersistentArrayMap) ednContent.get(CAUSE);
    Assert.assertEquals(sanitized ? "" : "The ice cream has melted!", causeContent.get(MESSAGE));
    PersistentArrayMap causeInfo = (PersistentArrayMap) causeContent.get(INFO);
    Assert.assertEquals(ExceptionInfo.class.getName(), causeInfo.get(EX_TYPE));
    if (!sanitized) {
      PersistentArrayMap causeData = (PersistentArrayMap) causeContent.get(DATA);
      PersistentHashSet causes = (PersistentHashSet) causeData.get(Keyword.intern("causes"));
      Assert.assertTrue(causes.contains(Keyword.intern("fridge-door-open")));
      Assert.assertTrue(causes.contains(Keyword.intern("dangerously-high-temperature")));
      PersistentArrayMap currentTemperature =
          (PersistentArrayMap) causeData.get(Keyword.intern("current-temperature"));
      Assert.assertEquals(25L, currentTemperature.get(Keyword.intern("value")));
      Assert.assertEquals(
          Keyword.intern("celsius"), currentTemperature.get(Keyword.intern("unit")));
    } else {
      Assert.assertEquals(PersistentArrayMap.create(new HashMap()), causeContent.get(DATA));
    }
  }

  @Test
  public void testTxFunctionThrowsGeneralException() throws Throwable {
    Mockito.when(environmentService.sanitizeExceptions()).thenReturn(false);
    txFunctionThrowsGeneralExceptionHelper(false);
  }

  private void txFunctionThrowsGeneralExceptionHelper(boolean sanitized) throws Throwable {
    controller.transact(
        "correlation-id",
        HEADERS,
        TENANT,
        CATEGORY,
        CONNECTION_REF_STRING,
        Transactions.generalExceptionTxFunction);
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/transact/{tenant}/{category}", TENANT, CATEGORY)
                    .header("_cid", LABEL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "transaction", Transactions.callGeneralExceptionTxFunction),
                                    new BasicNameValuePair("reference", CONNECTION_REF_STRING))))))
            .andExpect(
                status().is((int) EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getHttpErrorCode()))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);
    Assert.assertEquals(
        sanitized ? "" : "A tx-fn threw an exception: aborting transaction.",
        ednContent.get(MESSAGE));
    PersistentArrayMap ednExInfo = (PersistentArrayMap) ednContent.get(INFO);
    Assert.assertEquals("A transaction function threw an exception.", ednExInfo.get(EXPLANATION));
    Assert.assertEquals("TransactionFunctionException", ednExInfo.get(EX_TYPE));
    Assert.assertEquals(
        EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
        Math.toIntExact((Long) ednExInfo.get(CODE)));
    if (!sanitized) {
      PersistentArrayMap ednExData = (PersistentArrayMap) ednContent.get(DATA);
      Assert.assertEquals("throw-ex", ednExData.get(FN));
      Assert.assertEquals(
          Keyword.intern("transaction-pipeline/tx-fn-threw"), ednExData.get(EVA_ERROR));
      Assert.assertEquals(
          EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
          Math.toIntExact((Long) ednExData.get(EVA_ERROR_CODE)));
    } else {
      Assert.assertEquals(PersistentArrayMap.create(new HashMap()), ednContent.get(DATA));
    }
    PersistentArrayMap causeContent = (PersistentArrayMap) ednContent.get(CAUSE);
    Assert.assertEquals(sanitized ? "" : "RIP", causeContent.get(MESSAGE));
    PersistentArrayMap causeInfo = (PersistentArrayMap) causeContent.get(INFO);
    Assert.assertEquals(IllegalStateException.class.getName(), causeInfo.get(EX_TYPE));
  }

  @Test
  public void testTxFunctionThrowsNestedCauseException() throws Throwable {
    Mockito.when(environmentService.sanitizeExceptions()).thenReturn(false);
    Mockito.when(environmentService.exCauseNestingLimit()).thenReturn(2);
    txFunctionThrowsNestedCauseExceptionHelper(false);
  }

  private void txFunctionThrowsNestedCauseExceptionHelper(boolean sanitized) throws Throwable {
    controller.transact(
        "correlation-id",
        HEADERS,
        TENANT,
        CATEGORY,
        CONNECTION_REF_STRING,
        Transactions.generalNestedExceptionTxFunction);
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/transact/{tenant}/{category}", TENANT, CATEGORY)
                    .header("_cid", LABEL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "transaction",
                                        Transactions.callGeneralNestedExceptionTxFunction),
                                    new BasicNameValuePair("reference", CONNECTION_REF_STRING))))))
            .andExpect(
                status().is((int) EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getHttpErrorCode()))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);
    Assert.assertEquals(
        sanitized ? "" : "A tx-fn threw an exception: aborting transaction.",
        ednContent.get(MESSAGE));
    PersistentArrayMap ednExInfo = (PersistentArrayMap) ednContent.get(INFO);
    Assert.assertEquals("A transaction function threw an exception.", ednExInfo.get(EXPLANATION));
    Assert.assertEquals("TransactionFunctionException", ednExInfo.get(EX_TYPE));
    Assert.assertEquals(
        EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
        Math.toIntExact((Long) ednExInfo.get(CODE)));
    if (!sanitized) {
      PersistentArrayMap ednExData = (PersistentArrayMap) ednContent.get(DATA);
      Assert.assertEquals("throw-ex-nested", ednExData.get(FN));
      Assert.assertEquals(
          Keyword.intern("transaction-pipeline/tx-fn-threw"), ednExData.get(EVA_ERROR));
      Assert.assertEquals(
          EvaErrorCode.TRANSACTION_FUNCTION_EXCEPTION.getCode(),
          Math.toIntExact((Long) ednExData.get(EVA_ERROR_CODE)));
    } else {
      Assert.assertEquals(PersistentArrayMap.create(new HashMap()), ednContent.get(DATA));
    }
    PersistentArrayMap causeContent = (PersistentArrayMap) ednContent.get(CAUSE);
    Assert.assertEquals(sanitized ? "" : "RIP", causeContent.get(MESSAGE));
    PersistentArrayMap causeInfo = (PersistentArrayMap) causeContent.get(INFO);
    Assert.assertEquals(IllegalStateException.class.getName(), causeInfo.get(EX_TYPE));
    PersistentArrayMap nestedCauseContent = (PersistentArrayMap) causeContent.get(CAUSE);
    Assert.assertEquals(sanitized ? "" : "hello", nestedCauseContent.get(MESSAGE));
    PersistentArrayMap nestedCauseInfo = (PersistentArrayMap) nestedCauseContent.get(INFO);
    Assert.assertEquals(RuntimeException.class.getName(), nestedCauseInfo.get(EX_TYPE));
  }

  @Test
  public void testMimeException() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/latestT/{tenant}/{category}", "test", "test")
                    .header("_cid", "test")
                    .accept("application/vnd.eva+wrong")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "reference",
                                        "#eva.client.service/snapshot-ref { :label \"test\" }"))))))
            .andExpect(status().is(415))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    Object ednContent = Util.read(content);
    Assert.assertEquals("Mime type: application/vnd.eva+wrong is not supported", ednContent);
  }

  @Test
  public void testClientServiceException() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/q/{tenant}/{category}", "test", "test")
                    .header("_cid", "test")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "query", "[:find ?attr :in $ :where [_ :db/ident ?attr]]"),
                                    new BasicNameValuePair(
                                        "p[0]",
                                        "#eva.client.service/snapshot-ref { :label \"test\" :as-of #eva.client.service/inline { :fn first :params [[]] } }"))))))
            .andExpect(status().is(400))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);
    Assert.assertEquals("Error executing function: first", ednContent.get(MESSAGE));
  }

  @Test
  public void testRuntimeException() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/eva/v.1/q/{tenant}/{category}", "test", "test")
                    .header("_cid", "test")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(
                        EntityUtils.toString(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair(
                                        "query", "[:find ?attr :in $ :where [_ :db/ident ?attr]"),
                                    new BasicNameValuePair(
                                        "p[0]",
                                        "#eva.client.service/snapshot-ref { :label \"test\" }"))))))
            .andExpect(status().is(500))
            .andReturn();
    String content = result.getResponse().getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);
    Assert.assertEquals("EOF while reading", ednContent.get(MESSAGE));
  }
}
