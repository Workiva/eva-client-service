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
import org.junit.Assert;
import org.junit.Test;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.TestEnvironmentService;
import com.workiva.eva.clientservice.peer.TestPeerRepository;

import java.util.List;

public class UtilFunctionsTest {

  static TestPeerRepository peerRepository = new TestPeerRepository();
  static TestEnvironmentService environmentService = new TestEnvironmentService(true, true, 1);
  static final RequestContext testContext = new RequestContext.Builder().build();

  @Test
  public void firstTest() {
    List l = PersistentVector.create("test", "12345");
    Object results = UtilFunctions.first(peerRepository, l, testContext);
    Assert.assertEquals("test", results);
  }

  @Test(expected = java.lang.IndexOutOfBoundsException.class)
  public void firstNilTest() {
    List l = PersistentVector.create();
    UtilFunctions.first(peerRepository, l, testContext);
  }

  @Test
  public void ffirstTest() {
    List l = PersistentVector.create("test", "12345");
    List l2 = PersistentVector.create(l, "6789");
    Object results = UtilFunctions.ffirst(peerRepository, l2, testContext);
    Assert.assertEquals("test", results);
  }

  @Test(expected = java.lang.IndexOutOfBoundsException.class)
  public void ffirstNilTest() {
    List l = PersistentVector.create();
    UtilFunctions.first(peerRepository, l, testContext);
  }
}
