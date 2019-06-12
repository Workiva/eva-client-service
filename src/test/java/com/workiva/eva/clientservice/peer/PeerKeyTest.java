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

import org.junit.Test;

import static org.junit.Assert.*;

public class PeerKeyTest {

  private static final String testTen = "ten";
  private static final String testCat = "cat";
  private static final String testLabel = "label";

  @Test
  public void generalConstructionTest() {

    PeerKey key = new PeerKey(testTen, testCat, testLabel);

    assertNotNull(key);
    assertEquals("test tenant", testTen, key.getTenant());
    assertEquals("test category", testCat, key.getCategory());
    assertEquals("test label", testLabel, key.getLabel());
    assertTrue(key.isValid());
  }

  // TODO inconsistent, fails after first test run
  @Test
  public void validityTest() {
    PeerKey key1 = new PeerKey("", testCat, testLabel);
    assertNotNull(key1);
    assertFalse(key1.isValid());

    PeerKey key2 = new PeerKey(testTen, "", testLabel);
    assertNotNull(key2);
    assertFalse(key2.isValid());

    PeerKey key3 = new PeerKey(testTen, testCat, "");
    assertNotNull(key3);
    assertFalse(key3.isValid());

    PeerKey key4 = new PeerKey(null, testCat, testLabel);
    assertNotNull(key4);
    assertFalse(key4.isValid());

    PeerKey key5 = new PeerKey(testTen, null, testLabel);
    assertNotNull(key5);
    assertFalse(key5.isValid());

    PeerKey key6 = new PeerKey(testTen, testCat, null);
    assertNotNull(key6);
    assertFalse(key6.isValid());
  }

  @Test
  public void equalsTest() {
    PeerKey key1 = new PeerKey(testTen, testCat, testLabel);
    PeerKey key2 = new PeerKey("", testCat, testLabel);
    PeerKey key3 = new PeerKey(testTen, null, testLabel);
    PeerKey key4 = new PeerKey(testTen, testCat, testLabel);

    assertEquals(key1, key1);
    assertEquals(key2, key2);
    assertEquals(key3, key3);
    assertEquals(key4, key4);

    assertNotEquals(key1, key2);
    assertNotEquals(key1, key3);
    assertNotEquals(key1, key3);
    assertNotEquals(key1, null);
    assertNotEquals(key1, "WRONG");

    assertNotEquals(key2, key1);
    assertNotEquals(key2, key3);
    assertNotEquals(key2, key4);
    assertNotEquals(key2, null);
    assertNotEquals(key2, "WRONG");

    assertNotEquals(key3, key1);
    assertNotEquals(key3, key2);
    assertNotEquals(key3, key4);
    assertNotEquals(key3, null);
    assertNotEquals(key3, "WRONG");

    assertEquals(key4, key1);
    assertNotEquals(key4, key2);
    assertNotEquals(key4, key3);
    assertNotEquals(key4, null);
    assertNotEquals(key4, "WRONG");
  }

  @Test
  public void hashTest() {
    PeerKey key1 = new PeerKey(testTen, testCat, testLabel);
    PeerKey key2 = new PeerKey("", testCat, testLabel);
    PeerKey key3 = new PeerKey(testTen, null, testLabel);
    PeerKey key4 = new PeerKey(testTen, testCat, testLabel);

    assertNotEquals(key1.hashCode(), key2.hashCode());
    assertNotEquals(key1.hashCode(), key3.hashCode());
    assertEquals(key1.hashCode(), key4.hashCode());
  }
}
