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

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import com.workiva.eva.clientservice.exceptions.ClientServiceException;
import com.workiva.eva.clientservice.exceptions.MimeException;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/** Defines the serialization utilities. */
public class SerializerUtils {

  /** The `Accept` header value. */
  public static final String ACCEPT_HEADER = "Accept";

  /** Holds the default mime type. */
  public static final String DEFAULT_MIME_TYPE = EdnSerializer.EDN_MIME_TYPE;

  /** Holds the universal mime type. */
  public static final String UNIVERSAL_MIME_TYPE = "*/*";

  /** Holds the collection of serializer. */
  private static final Map<String, Serializer> serializers;

  static {
    Serializer ednSerializer = new EdnSerializer();

    serializers =
        new HashMap<String, Serializer>() {
          {
            put(UNIVERSAL_MIME_TYPE, ednSerializer);
            put(DEFAULT_MIME_TYPE, ednSerializer);
          }
        };
  }

  /**
   * Get the mime type from the header collection.
   *
   * @param headers The headers to examine.
   * @return Returns mime type.
   */
  public static MimeType getMimeType(Map<String, String> headers) {
    // TODO - treemap everywhere if possible instead of converting here
    // Http Header names are case insensitive - https://tools.ietf.org/html/rfc7230#appendix-A.2
    TreeMap<String, String> caseInsensitiveHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    caseInsensitiveHeaders.putAll(headers);

    String raw = caseInsensitiveHeaders.getOrDefault(ACCEPT_HEADER, DEFAULT_MIME_TYPE);
    MimeType mimeType = MimeTypeUtils.parseMimeType(raw);

    String key = String.format("%s/%s", mimeType.getType(), mimeType.getSubtype());
    if (!serializers.containsKey(key)) {
      throw new MimeException("Mime type: %s is not supported", key);
    }

    return mimeType;
  }

  /**
   * Get the serializer from the collection.
   *
   * @param mimeType The mime type to look for.
   * @return Returns the serializer.
   */
  private static Serializer getSerializer(MimeType mimeType) {
    Serializer serializer =
        serializers.get(String.format("%s/%s", mimeType.getType(), mimeType.getSubtype()));
    if (serializer == null) {
      throw new MimeException("Mime type: %s is not implemented", mimeType);
    }
    return serializer;
  }

  /**
   * Serialize the object with the specified header.
   *
   * @param headers The header which may control the returned format.
   * @param toSerialize The object to serialize.
   * @return Returns the serialized value.
   */
  public static String serialize(Map<String, String> headers, Object toSerialize) {
    MimeType mimeType = getMimeType(headers);
    Serializer serializer = getSerializer(mimeType);
    return serializer.serialize(mimeType.getParameters(), toSerialize);
  }

  /**
   * Deserialize the edn into a list.
   *
   * @param headers Indicate controls to the deserialization.
   * @param data The string to deserialize.
   * @param deserializers the collection of deserializers.
   * @return Returns the list.
   */
  public static <T> T deserialize(
      Map<String, String> headers, String data, Map<String, Function> deserializers)
      throws ClientServiceException {
    MimeType mimeType = getMimeType(headers);
    Serializer serializer = getSerializer(mimeType);
    return serializer.deserialize(mimeType.getParameters(), data, deserializers);
  }

  /**
   * Deserialize the edn into a list.
   *
   * @param headers Indicate controls to the deserialization.
   * @param data The string to deserialize.
   * @return Returns the list.
   */
  public static <T> T deserialize(Map<String, String> headers, String data)
      throws ClientServiceException {
    MimeType mimeType = getMimeType(headers);
    Serializer serializer = getSerializer(mimeType);
    return serializer.deserialize(mimeType.getParameters(), data, null);
  }
}
