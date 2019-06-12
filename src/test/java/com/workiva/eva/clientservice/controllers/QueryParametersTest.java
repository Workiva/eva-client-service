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

import eva.Connection;
import eva.Database;
import org.junit.Assert;
import org.junit.Test;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.peer.TestPeerRepository;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.util.HashMap;
import java.util.Map;

import static com.workiva.eva.clientservice.TestUtils.CATEGORY;
import static com.workiva.eva.clientservice.TestUtils.CONNECTION_REF_STRING;
import static com.workiva.eva.clientservice.TestUtils.SNAP_REF_STRING;
import static com.workiva.eva.clientservice.TestUtils.TENANT;

public class QueryParametersTest {

  static TestPeerRepository peerRepository = new TestPeerRepository();
  static TestEnvironmentService environmentService = new TestEnvironmentService(true, true, 1);

  @Test
  public void getParametersTest() {
    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("0", "test-ref");
            put("1", "0");
          }
        };

    QueryParameters params = new QueryParameters();
    params.setP(rawParams);
    Map<String, String> paramResult = params.getP();
    Assert.assertEquals("test-ref", paramResult.get("0"));
    Assert.assertEquals("0", paramResult.get("1"));
  }

  @Test
  public void connectionAndSnapshotParameterTest() {
    Map<String, String> headers =
        new HashMap<String, String>() {
          {
            put(SerializerUtils.ACCEPT_HEADER, SerializerUtils.DEFAULT_MIME_TYPE);
          }
        };

    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("0", CONNECTION_REF_STRING);
            put("1", SNAP_REF_STRING);
          }
        };

    QueryParameters params = new QueryParameters();
    params.setP(rawParams);

    Object[] convertedParams =
        params.all(
            new RequestContext.Builder().httpHeaders(headers).build(),
            peerRepository,
            TENANT,
            CATEGORY);
    Assert.assertTrue(convertedParams[0] instanceof Connection);
    Assert.assertTrue(convertedParams[1] instanceof Database);
  }

  @Test
  public void queryParameterToStringTest() {
    Map<String, String> rawParams =
        new HashMap<String, String>() {
          {
            put("0", CONNECTION_REF_STRING);
            put("1", SNAP_REF_STRING);
          }
        };

    QueryParameters params = new QueryParameters();
    params.setP(rawParams);

    String expected =
        String.format("{'0' = [%s] , '1' = [%s] }", CONNECTION_REF_STRING, SNAP_REF_STRING);
    Assert.assertEquals(expected, params.toString());
  }
}
