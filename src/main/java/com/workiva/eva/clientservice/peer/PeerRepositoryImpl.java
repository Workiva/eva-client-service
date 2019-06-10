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

import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import com.codahale.metrics.MetricRegistry;
import eva.Connection;
import eva.Database;
import eva.Peer;
import eva.catalog.client.alpha.HTTPCatalogClientImpl;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.analytics.Telemetry;
import com.workiva.eva.clientservice.exceptions.ClientServiceException;
import com.workiva.eva.clientservice.exceptions.InvalidReferenceException;
import com.workiva.eva.clientservice.exceptions.NotFoundException;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.reference.Reference;
import com.workiva.eva.clientservice.reference.SnapshotReference;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Defines the implementation to the peer repository. */
@Component
public class PeerRepositoryImpl implements PeerRepository {

  class IdentifierPair {
    final UUID dbId;
    final UUID partitionId;

    IdentifierPair() {
      this.dbId = UUID.randomUUID();
      this.partitionId = UUID.randomUUID();
    }
  }

  /** Defines the LOGGER. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PeerRepositoryImpl.class);

  /** The collection of connections. */
  protected final ConcurrentHashMap<PeerKey, Connection> connections;

  // Facilitates multiple in-mem connections
  private ConcurrentHashMap<PeerKey, IdentifierPair> localConfigurations;

  /** The service endpoint. */
  @Value("${eva.catalog:LOCAL}")
  protected String catalogUrl;

  /** Create an instance of the peer repository. */
  PeerRepositoryImpl() {
    this.connections = new ConcurrentHashMap<>();
    this.localConfigurations = new ConcurrentHashMap<>();

    String connectionNumGaugeName = MetricRegistry.name(PeerRepositoryImpl.class, "connections");
    Telemetry.registerGauge(connectionNumGaugeName, connections::size);
  }

  protected Map getLocalInMemConfig(RequestContext parentCtx, IdentifierPair identifierPair) {
    try (RequestContext ctx = parentCtx.startSpan("getLocalInMemConfig")) {
      return PersistentHashMap.create(
          Keyword.intern("local"), true,
          Keyword.intern("eva.v2.storage.value-store.core/partition-id"),
              identifierPair.partitionId,
          Keyword.intern("eva.v2.database.core/id"), identifierPair.dbId);
    }
  }

  /**
   * Get the connection from label, category and tenant. If it doesn't exist, get if from the
   * catalog service.
   *
   * @param parentCtx propagated application context
   * @param tenant Tenant portion of reference
   * @param category Category portion of reference
   * @param label Label portion of reference
   * @return Returns the connection.
   */
  private Connection getConnection(
      RequestContext parentCtx, String tenant, String category, String label) {
    try (RequestContext ctx = parentCtx.startSpan("getConnection")) {
      Connection connection = null;
      PeerKey key = new PeerKey(tenant, category, label);
      if (key.isValid()) {
        final long startTime = System.currentTimeMillis();
        connection = this.connections.get(key);
        if (connection == null) {
          LOGGER.info(
              "Creating connection to [Tenant: {}, Category: {}, Label: {}]",
              tenant,
              category,
              label);

          Map config;
          if (catalogUrl != null && !this.catalogUrl.equals("LOCAL")) {
            Span span =
                GlobalTracer.get()
                    .buildSpan("eva-catalog/requestFlatPeerConfig")
                    .asChildOf(ctx.getSpan())
                    .start();
            config =
                HTTPCatalogClientImpl.requestFlatPeerConfig(
                    this.catalogUrl, tenant, category, label);
            span.finish();
          } else {
            config = getLocalInMemConfig(ctx, getIdentifierPair(key));
          }

          Span span =
              GlobalTracer.get()
                  .buildSpan("eva-peer/Peer.connect")
                  .asChildOf(ctx.getSpan())
                  .start();
          connection = Peer.connect(config);
          span.finish();
          // TODO - only cache if we get something back!
          this.connections.putIfAbsent(key, connection);
          Telemetry.updateTimer(
              MetricRegistry.name(PeerRepositoryImpl.class, "getConnection"), startTime);
        }
      } else {
        LOGGER.info(
            "Invalid connection info: [Tenant: {}, Category: {}, Label: {}]",
            tenant,
            category,
            label);
      }
      if (connection == null) {
        throw new NotFoundException();
      }
      return connection;
    }
  }

  /**
   * Get a connection from the peer repository.
   *
   * @param reference The label to the specific connection.
   * @return Returns the connection.
   * @throws ClientServiceException Thrown when errored.
   */
  @Override
  public Connection getConnection(RequestContext parentCtx, Object reference)
      throws ClientServiceException {
    if (reference instanceof ConnectionReference) {
      ConnectionReference ref = (ConnectionReference) reference;
      parentCtx.appendConnectionTags(ref);
      return getConnection(parentCtx, ref.tenant(), ref.category(), ref.label());
    } else {
      throw new InvalidReferenceException();
    }
  }

  /**
   * Get a database from the peer repository.
   *
   * @param reference The specific database snapshot reference, which may be an expression.
   * @return Returns the connection.
   * @throws ClientServiceException Thrown when an error occurs.
   */
  @Override
  public Database getSnapshot(RequestContext parentCtx, Object reference)
      throws ClientServiceException {
    if (reference instanceof Reference) {
      Connection conn = this.getConnection(parentCtx, reference);
      if (reference instanceof SnapshotReference) {
        SnapshotReference ref = (SnapshotReference) reference;
        parentCtx.appendConnectionTags(ref);
        return ref.getSnapshot(parentCtx, conn);
      } else {
        return conn.db();
      }
    } else {
      throw new InvalidReferenceException();
    }
  }

  protected IdentifierPair getIdentifierPair(PeerKey key) {
    return localConfigurations.computeIfAbsent(key, (PeerKey k) -> new IdentifierPair());
  }
}
