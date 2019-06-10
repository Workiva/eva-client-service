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

/** Defines the connection reference. */
public class ConnectionReference implements Reference {

  /** Holds the tenant. */
  private String tenant;

  /** Holds the category. */
  private String cat;

  /** Holds the label. */
  private String label;

  /**
   * Create a connection reference.
   *
   * @param tenant The tenant of the connection.
   * @param cat The category of the connection.
   * @param label The label of the connection.
   */
  public ConnectionReference(String tenant, String cat, String label) {
    this.tenant = tenant;
    this.cat = cat;
    this.label = label;
  }

  /**
   * Get the database label.
   *
   * @return Returns the label.
   */
  @Override
  public String label() {
    return this.label;
  }

  /**
   * Get the tenant.
   *
   * @return Returns the tenant.
   */
  @Override
  public String tenant() {
    return this.tenant;
  }

  /**
   * Get the category.
   *
   * @return Returns the category.
   */
  @Override
  public String category() {
    return this.cat;
  }

  @Override
  public String toString() {
    String connectionRef = ReferenceUtils.CONNECTION_REFERENCE_TAG;
    return String.format("#%s { :label \"%s\" }", connectionRef, label());
  }
}
