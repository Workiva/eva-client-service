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

package com.workiva.eva.clientservice.controllers.v1;

import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import eva.Alpha;
import eva.Attribute;
import eva.Connection;
import eva.Database;
import eva.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ValueConstants;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.config.EnvironmentService;
import com.workiva.eva.clientservice.controllers.ControllerUtils;
import com.workiva.eva.clientservice.controllers.QueryParameters;
import com.workiva.eva.clientservice.peer.PeerRepository;
import com.workiva.eva.clientservice.peer.PeerVersion;
import com.workiva.eva.clientservice.reference.ConnectionReference;
import com.workiva.eva.clientservice.reference.Reference;
import com.workiva.eva.clientservice.reference.ReferenceUtils;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Defines the controller for the pull operations. */
@Controller
@PeerVersion("v.1")
@RequestMapping(path = {"eva/v.1", "s/eva-client-service/eva/v.1"})
public class PeerControllerV1 {

  /** Holds the tempid keyword. */
  private static final Keyword TEMPID_KEY = Keyword.intern("eva.client.service/tempids");

  /** Defines the LOGGER. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PeerControllerV1.class);

  /** Holds the peer repository. */
  private PeerRepository peerRepository;

  /** Holds all relevant environment variables. */
  private EnvironmentService environment;

  /** The names of the various Peer library endpoints. */
  private static final String TRANSACT = "transact";

  private static final String WITH = "with";
  private static final String QUERY = "query";
  private static final String PULL = "pull";
  private static final String DATOMS = "datoms";
  private static final String LATEST_T = "latestT";
  private static final String TX_RANGE = "tx-range";
  private static final String INVOKE = "invoke";
  private static final String ENTID = "entid";
  private static final String IDENT = "ident";
  private static final String ATTRIBUTE = "attribute";
  private static final String EXTANT_ENTITY = "extant-entity";

  /** Additional endpoints. */

  /**
   * Create the controller.
   *
   * @param peerRepository The peer repository to use.
   */
  @Autowired
  PeerControllerV1(PeerRepository peerRepository, EnvironmentService environmentService) {
    this.peerRepository = peerRepository;
    this.environment = environmentService;
  }

  /**
   * Applies formatting.
   *
   * @param parentCtx The propagated RequestContext.
   * @param call The call being executed.
   * @param response The function to run.
   * @return Returns the request response.
   */
  private String formatResult(
      RequestContext parentCtx, String call, Map<String, Object> params, Object response) {
    if (TRANSACT.equals(call) || WITH.equals(call)) {
      String tenant = (String) params.get("tenant");
      String category = (String) params.get("category");
      String reference = (String) params.get("reference");
      ConnectionReference connRef =
          ReferenceUtils.createConnectionReferenceFromString(
              parentCtx, tenant, category, reference);
      ReferenceUtils.replaceDbWithSnapshotReference(parentCtx, (Map) response, connRef);
    }
    return SerializerUtils.serialize(parentCtx.getHttpHeaders(), response);
  }

  /**
   * Transact the contents.
   *
   * @param correlationId The correlation id.
   * @param reference The reference containning the label.
   * @param category The category identifier.
   * @param tenant The label identifier (database reference).
   * @param transaction The content of the transaction (only used if Mime is an a webform value).
   * @return Returns the contents as EDN.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "transact/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String transact(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("transaction") final String transaction)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("transaction", transaction);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    try (MDC.MDCCloseable call = MDC.putCloseable("call", TRANSACT);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(TRANSACT),
          TRANSACT,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Connection conn =
                this.peerRepository.getConnection(
                    parentCtx,
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference));

            List tx =
                (List)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, transaction);
            Map result = conn.transact(tx).get();

            Map<Object, Object> results = new HashMap<>();
            result.forEach(results::put);

            results.put(TEMPID_KEY, Alpha.txResultToEntityIDTempIDMap(result));
            return formatResult(parentCtx, TRANSACT, params, results);
          });
    } finally {
      ControllerUtils.removeReferenceMetadata();
    }
  }

  /**
   * Simulates a transaction locally without persisting the updated state.
   *
   * @param correlation The correlation id.
   * @param reference The reference containing the label.
   * @param category The category identifier.
   * @param tenant The label identifier (database reference).
   * @param transaction The content of the transaction (only used if Mime is an a webform value).
   * @return Returns the contents as EDN.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "with/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String with(
      @RequestHeader(name = "_cid", required = false) final String correlation,
      @RequestHeader Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("transaction") final String transaction)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("transaction", transaction);
    try (MDC.MDCCloseable logPath = MDC.putCloseable("path", WITH); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(WITH),
          WITH,
          correlation,
          params,
          (RequestContext parentCtx) -> {
            Database db =
                this.peerRepository.getSnapshot(
                    parentCtx,
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference));
            List tx = SerializerUtils.deserialize(headers, transaction, null);
            Map result = db.with(tx);

            Map<Object, Object> results = new HashMap<>();
            result.forEach(results::put);

            results.put(TEMPID_KEY, Alpha.txResultToEntityIDTempIDMap(result));
            return formatResult(parentCtx, WITH, params, results);
          });
    }
  }

  /**
   * Query the connection.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param query The query itself.
   * @param queryParams The fields used by the query.
   * @return Returns the query results.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "q/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String query(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("query") final String query,
      @ModelAttribute("p") final QueryParameters queryParams)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("query", query);
    params.put("params", queryParams);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    final int[] referenceCounters = {0, 0};
    try (MDC.MDCCloseable call = MDC.putCloseable("call", QUERY);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(QUERY),
          QUERY,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Object[] resolvedQueryParams =
                queryParams.all(parentCtx, this.peerRepository, tenant, category);
            ReferenceUtils.countSnapshotAndConnectionReferences(
                resolvedQueryParams, referenceCounters);
            Object result =
                Peer.query(SerializerUtils.deserialize(headers, query, null), resolvedQueryParams);
            return formatResult(parentCtx, QUERY, params, result);
          });
    } finally {
      ControllerUtils.removeQueryParamReferenceFromMDC(referenceCounters);
    }
  }

  /**
   * pull from a snapshot.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The database reference.
   * @param ids The ids to pull from (or possibly a query that returns ids).
   * @param pattern The pull pattern.
   * @return Returns the requested items.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "pull/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String pull(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam(name = "ids", required = false) final String ids,
      @RequestParam("pattern") final String pattern,
      @ModelAttribute("p") final QueryParameters queryParams)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("ids", ids);
    params.put("pattern", pattern);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    int[] referenceCounters = {0, 0};
    try (MDC.MDCCloseable call = MDC.putCloseable("call", PULL);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(PULL),
          PULL,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Database db =
                this.peerRepository.getSnapshot(
                    parentCtx,
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference));

            Object result;
            Object q = SerializerUtils.deserialize(parentCtx.getHttpHeaders(), pattern);
            Object realIds = SerializerUtils.deserialize(parentCtx.getHttpHeaders(), ids);

            if (realIds instanceof List) {
              result = db.pullMany(q, (List) realIds);
            } else {
              result = db.pull(q, realIds);
            }

            return formatResult(parentCtx, PULL, params, result);
          });
    } finally {
      ControllerUtils.removeQueryParamReferenceFromMDC(referenceCounters);
    }
  }

  /**
   * Provides raw access to the database indexes. Must pass the index-name. May pass one or more
   * leading components of the index to constrain the results.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The reference containing the label.
   * @param index The index to be searched.
   * @param components A component on the datom to be used to narrow down the search.
   * @return Returns the raw selected datoms.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "datoms/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String datoms(
      @RequestHeader(name = "_cid", required = false) final String correlationId,
      @RequestHeader Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("index") final String index,
      @RequestParam("components") final String components)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("index", index);
    params.put("components", components);
    try (MDC.MDCCloseable logPath = MDC.putCloseable("path", DATOMS); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(DATOMS),
          DATOMS,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            Database db = this.peerRepository.getSnapshot(parentCtx, ref);
            Keyword indexDeserialized =
                SerializerUtils.deserialize(parentCtx.getHttpHeaders(), index);
            Object deserializedComponents =
                SerializerUtils.deserialize(parentCtx.getHttpHeaders(), components);
            ArrayList<Object> temp = new ArrayList<>();
            if (deserializedComponents instanceof List) {
              for (Object o : (List) deserializedComponents) {
                temp.add(o);
              }
            } else {
              temp.add(deserializedComponents);
            }
            return formatResult(
                parentCtx, DATOMS, params, db.datoms(indexDeserialized, temp.toArray()));
          });
    }
  }

  /**
   * Returns the transaction log entries for the given or computed range.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The reference containing the label.
   * @param start The starting tx number.
   * @param end The ending tx number, not inclusive, if omitted will use the value returned by
   *     latest-t + 1.
   * @return Returns the log entries within the given tx range.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "tx-range/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String txRange(
      @RequestHeader(name = "_cid", required = false) final String correlationId,
      @RequestHeader Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("start") final Long start,
      @RequestParam(name = "end", required = false, defaultValue = "-1") final Long end)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("start", start);
    params.put("end", end);
    try (MDC.MDCCloseable logPath = MDC.putCloseable("path", TX_RANGE); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(TX_RANGE),
          TX_RANGE,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            Connection conn = this.peerRepository.getConnection(parentCtx, ref);
            if (end < 0) {
              Long latestT =
                  SerializerUtils.deserialize(
                      parentCtx.getHttpHeaders(),
                      latestT(correlationId, headers, ref.toString(), tenant, category));
              if (start.equals(latestT)) {
                latestT++;
              }
              return formatResult(parentCtx, TX_RANGE, params, conn.log().txRange(start, latestT));
            } else {
              return formatResult(parentCtx, TX_RANGE, params, conn.log().txRange(start, end));
            }
          });
    }
  }

  /**
   * Returns the latest transaction number for a particular connection config.
   *
   * @param correlation The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param label The label identifier.
   * @return Returns the latest tx number.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "latestT/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String latestT(
      @RequestHeader(name = "_cid", required = false) final String correlation,
      @RequestHeader Map<String, String> headers,
      @RequestParam("reference") final String reference,
      @PathVariable final String tenant,
      @PathVariable final String category)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    try (MDC.MDCCloseable logPath = MDC.putCloseable("path", LATEST_T); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(LATEST_T),
          LATEST_T,
          correlation,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            Connection conn = this.peerRepository.getConnection(parentCtx, ref);
            return formatResult(parentCtx, LATEST_T, params, conn.latestT());
          });
    }
  }

  /**
   * Invoke a transaction function.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The label identifier (database reference).
   * @param function the function to invoke.
   * @param queryParams The fields used by the invoke.
   * @return Returns the requested items.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "invoke/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String invoke(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader final Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("function") final String function,
      @ModelAttribute("p") final QueryParameters queryParams)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("function", function);
    params.put("params", queryParams);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    int[] referenceCounters = {0, 0};
    try (MDC.MDCCloseable call = MDC.putCloseable("call", INVOKE);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(INVOKE),
          INVOKE,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            Database db = this.peerRepository.getSnapshot(parentCtx, ref);
            ControllerUtils.addReferenceMetadata(ref);

            Object[] resolvedQueryParams =
                queryParams.all(parentCtx, this.peerRepository, tenant, category);
            ReferenceUtils.countSnapshotAndConnectionReferences(
                resolvedQueryParams, referenceCounters);

            Object result =
                db.invoke(SerializerUtils.deserialize(headers, function), resolvedQueryParams);
            return formatResult(parentCtx, INVOKE, params, result);
          });
    } finally {
      ControllerUtils.removeQueryParamReferenceFromMDC(referenceCounters);
      ControllerUtils.removeReferenceMetadata();
    }
  }

  /**
   * Coerces any entity-identifier into an entity-id. Does not confirm existence of an entity id,
   * except incidentally through some coercion processes.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The label identifier (database reference).
   * @param ident The id to be perform entid with.
   * @param strict Whether or not to throw an exception if the entid is not found.
   * @return Returns the requested items.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "entid/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String entid(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader final Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("ident") final String ident,
      @RequestParam(name = "strict", required = false) final boolean strict)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("ident", ident);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    int[] referenceCounters = {0, 0};
    try (MDC.MDCCloseable call = MDC.putCloseable("call", ENTID);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(ENTID),
          ENTID,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            ControllerUtils.addReferenceMetadata(ref);
            Database db = this.peerRepository.getSnapshot(parentCtx, ref);
            Object result;
            if (strict) {
              result = db.entidStrict(SerializerUtils.deserialize(headers, ident));
            } else {
              result = db.entid(SerializerUtils.deserialize(headers, ident));
            }
            return formatResult(parentCtx, ENTID, params, result);
          });
    } finally {
      ControllerUtils.removeQueryParamReferenceFromMDC(referenceCounters);
      ControllerUtils.removeReferenceMetadata();
    }
  }

  /**
   * Returns the keyword-identifier associated with an id.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The label identifier (database reference).
   * @param entid The id to be perform ident with.
   * @param strict Whether or not to throw an exception if the ident is not found.
   * @return Returns the requested items.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "ident/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String ident(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader final Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("entid") final String entid,
      @RequestParam(name = "strict", required = false) final boolean strict)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("entid", entid);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    int[] referenceCounters = {0, 0};
    try (MDC.MDCCloseable call = MDC.putCloseable("call", IDENT);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(IDENT),
          IDENT,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            ControllerUtils.addReferenceMetadata(ref);
            Database db = this.peerRepository.getSnapshot(parentCtx, ref);
            Object result;
            if (strict) {
              result = db.identStrict(SerializerUtils.deserialize(headers, entid));
            } else {
              result = db.ident(SerializerUtils.deserialize(headers, entid));
            }
            return formatResult(parentCtx, IDENT, params, result);
          });
    } finally {
      ControllerUtils.removeQueryParamReferenceFromMDC(referenceCounters);
      ControllerUtils.removeReferenceMetadata();
    }
  }

  /**
   * Retrieves information about an Attribute.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The label identifier (database reference).
   * @param attrId The id of the attribute in question.
   * @param strict Whether or not to throw an error if nothing is found.
   * @return Returns the requested items.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "attribute/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String attribute(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader final Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("attrId") final String attrId,
      @RequestParam(name = "strict", required = false) final boolean strict)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("attrId", attrId);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    int[] referenceCounters = {0, 0};
    try (MDC.MDCCloseable call = MDC.putCloseable("call", ATTRIBUTE);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(ATTRIBUTE),
          ATTRIBUTE,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            ControllerUtils.addReferenceMetadata(ref);
            Database db = this.peerRepository.getSnapshot(parentCtx, ref);
            Attribute attr;
            if (strict) {
              attr = db.attributeStrict(SerializerUtils.deserialize(headers, attrId));
            } else {
              attr = db.attribute(SerializerUtils.deserialize(headers, attrId));
            }
            return formatResult(parentCtx, ATTRIBUTE, params, attr);
          });
    } finally {
      ControllerUtils.removeQueryParamReferenceFromMDC(referenceCounters);
      ControllerUtils.removeReferenceMetadata();
    }
  }

  /**
   * Returns true if there exists at least one datom in the database with the provided entity
   * identifier.
   *
   * @param correlationId The correlation id.
   * @param tenant The tenant identifier.
   * @param category The category identifier.
   * @param reference The label identifier (database reference).
   * @param ident The entity identifier
   * @return Returns the requested items.
   * @throws Throwable Thrown on any error.
   */
  @RequestMapping(path = "extant-entity/{tenant}/{category}", method = RequestMethod.POST)
  public @ResponseBody String extantEntity(
      @RequestHeader(name = "_cid", required = false) String correlationId,
      @RequestHeader final Map<String, String> headers,
      @PathVariable final String tenant,
      @PathVariable final String category,
      @RequestParam("reference") final String reference,
      @RequestParam("ident") final String ident)
      throws Throwable {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("tenant", tenant);
    params.put("category", category);
    params.put("reference", reference);
    params.put("ident", ident);
    if (correlationId == null || ValueConstants.DEFAULT_NONE.equals(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    int[] referenceCounters = {0, 0};
    try (MDC.MDCCloseable call = MDC.putCloseable("call", EXTANT_ENTITY);
        MDC.MDCCloseable cid = MDC.putCloseable("correlationId", correlationId); ) {
      return ControllerUtils.logRequestResponse(
          new RequestContext.Builder().httpHeaders(headers).build().startSpan(EXTANT_ENTITY),
          EXTANT_ENTITY,
          correlationId,
          params,
          (RequestContext parentCtx) -> {
            Reference ref =
                (Reference)
                    ReferenceUtils.deserializeEdn(
                        parentCtx, this.peerRepository, tenant, category, reference);
            ControllerUtils.addReferenceMetadata(ref);
            Database db = this.peerRepository.getSnapshot(parentCtx, ref);
            Object identDeserialized =
                SerializerUtils.deserialize(parentCtx.getHttpHeaders(), ident);
            return formatResult(
                parentCtx, EXTANT_ENTITY, params, db.isExtantEntity(identDeserialized));
          });
    } finally {
      ControllerUtils.removeQueryParamReferenceFromMDC(referenceCounters);
      ControllerUtils.removeReferenceMetadata();
    }
  }
}
