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

import java.util.Objects;

/** Defines the key used to look up a peer. */
public class PeerKey {

  /** Holds the tenant. */
  private final String tenant;

  /** Holds the category. */
  private final String category;

  /** Holds the label. */
  private final String label;

  /**
   * Create an instance of the peer key.
   *
   * @param tenant The tenant.
   * @param category The category.
   * @param label The label.
   */
  public PeerKey(String tenant, String category, String label) {
    this.tenant = tenant;
    this.category = category;
    this.label = label;
  }

  /**
   * Check if the key is valid.
   *
   * @return Returns true if the key is valid, false otherwise.
   */
  public boolean isValid() {

    // These rules are simplistic... maybe something more advanced?
    return this.getTenant() != null
        && !this.getTenant().isEmpty()
        && this.getCategory() != null
        && !this.getCategory().isEmpty()
        && this.getLabel() != null
        && !this.getLabel().isEmpty();
  }

  /**
   * Get the tenant value.
   *
   * @return Returns the tenant.
   */
  public String getTenant() {
    return this.tenant;
  }

  /**
   * Get the category value.
   *
   * @return Returns the category.
   */
  public String getCategory() {
    return this.category;
  }

  /**
   * Get the label value.
   *
   * @return Returns the subcategory.
   */
  public String getLabel() {
    return this.label;
  }

  /**
   * Check if the input equals this object.
   *
   * @param o The object to examine.
   * @return Returns true if the objects are equal, false otherwise.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PeerKey peerKey = (PeerKey) o;
    return Objects.equals(tenant, peerKey.tenant)
        && Objects.equals(category, peerKey.category)
        && Objects.equals(label, peerKey.label);
  }

  /**
   * Generate the hash code.
   *
   * @return Returns the hash code.
   */
  @Override
  public int hashCode() {
    int hash = Objects.hash(tenant, category, label);
    return hash;
  }
}
