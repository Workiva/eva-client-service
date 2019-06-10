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

import eva.Connection;
import eva.Database;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.exceptions.ClientServiceException;

/** Defines the wrapper for the Eva peers. */
public interface PeerRepository {

  /**
   * Get a connection from the peer repository.
   *
   * @param parentCtx Propagated request context
   * @param reference The specific connection reference.
   * @return Returns the connection.
   * @throws ClientServiceException Thrown when errored.
   */
  Connection getConnection(RequestContext parentCtx, Object reference)
      throws ClientServiceException;

  /**
   * Get a database from the peer repository.
   *
   * @param parentCtx Propagated request context
   * @param reference The specific database snapshot reference, which may be an expression.
   * @return Returns the connection.
   * @throws ClientServiceException Thrown when errored.
   */
  Database getSnapshot(RequestContext parentCtx, Object reference) throws ClientServiceException;
}
