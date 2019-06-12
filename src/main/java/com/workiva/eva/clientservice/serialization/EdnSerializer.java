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

package com.workiva.eva.clientservice.serialization;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import eva.Util;

import java.util.Map;
import java.util.function.Function;

/** Defines the serializer helpers for edn. */
public class EdnSerializer implements Serializer {

  static {
    IFn require = Clojure.var("clojure.core", "require");
    require.invoke(Clojure.read("eva.clientservice.references"));
  }

  /** Defines the EDN mime type. */
  public static final String EDN_MIME_TYPE = "application/vnd.eva+edn";

  /** Holds the internal serializer helper. */
  private static final IFn prStrVar = Clojure.var("clojure.core", "pr-str");

  /**
   * Serialize the object into edn.
   *
   * @param params Any control aspects of the serializer.
   * @param obj Defines the object.
   * @return Returns the serialized object.
   */
  public String serialize(Map<String, String> params, Object obj) {
    return (String) prStrVar.invoke(obj);
  }

  /**
   * Deserialize the edn into a list.
   *
   * @param params Indicate controls to the deserialization.
   * @param edn The string to deserialize.
   * @param deserializers the collection of deserializers.
   * @return Returns the list.
   */
  public <T> T deserialize(
      Map<String, String> params, String edn, Map<String, Function> deserializers) {
    T out = null;
    if (deserializers == null) {
      out = (T) Util.read(edn);
    } else {
      out = (T) Util.read(deserializers, edn);
    }
    return out;
  }
}
