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

package com.workiva.eva.clientservice;

import com.workiva.eva.clientservice.reference.ConnectionReference;

public class TestUtils {
  public static final String TENANT = "tenant";
  public static final String CATEGORY = "category";
  public static final String LABEL = "label";
  public static final String SNAP_REF_STRING =
      String.format("#eva.client.service/snapshot-ref { :label \"%s\" }", LABEL);
  public static final String CONNECTION_REF_STRING =
      String.format("#eva.client.service/connection-ref { :label \"%s\" }", LABEL);
  public static final ConnectionReference CONNECTION_REF =
      new ConnectionReference(TENANT, CATEGORY, LABEL);
}
