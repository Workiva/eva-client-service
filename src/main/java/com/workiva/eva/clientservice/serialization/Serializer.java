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

import com.workiva.eva.clientservice.exceptions.ClientServiceException;

import java.util.Map;
import java.util.function.Function;

/** Defines the serializer interface. */
public interface Serializer {

  /**
   * Serialize the object with the specified parameters. It is assumed that these _should_ come from
   * the mime type parameters.
   *
   * @param params The parameters which can control the outcome.
   * @param obj The object to serialize.
   * @return Returns the serialized form.
   */
  String serialize(Map<String, String> params, Object obj);

  /**
   * Deserialize the edn into a list.
   *
   * @param edn The string to deserialize.
   * @param deserializers the collection of deserializers.
   * @return Returns the list.
   */
  <T> T deserialize(Map<String, String> params, String edn, Map<String, Function> deserializers)
      throws ClientServiceException;
}
