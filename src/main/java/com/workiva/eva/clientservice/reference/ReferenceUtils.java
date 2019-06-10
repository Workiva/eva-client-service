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

import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import clojure.lang.Symbol;
import eva.Connection;
import eva.Database;
import eva.error.v1.EvaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;

import com.workiva.eva.clientservice.analytics.RequestContext;
import com.workiva.eva.clientservice.exceptions.ClientServiceException;
import com.workiva.eva.clientservice.exceptions.ExceptionUtils;
import com.workiva.eva.clientservice.exceptions.FunctionInvokeException;
import com.workiva.eva.clientservice.exceptions.ReferenceException;
import com.workiva.eva.clientservice.functions.PeerFunction;
import com.workiva.eva.clientservice.functions.PeerFunctionCollection;
import com.workiva.eva.clientservice.peer.PeerRepository;
import com.workiva.eva.clientservice.serialization.SerializerUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** The reference utilities. */
public class ReferenceUtils {

  /** Defines the LOGGER. */
  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceUtils.class);

  /** Holds the collection of functions. */
  private static Map<Symbol, BiFunction> functionCache;

  /** The set of strings defining tags to resolve. */
  static final String CONNECTION_REFERENCE_TAG = "eva.client.service/connection-ref";

  static final String SNAPSHOT_REFERENCE_TAG = "eva.client.service/snapshot-ref";
  private static final String INLINE_FUNC_TAG = "eva.client.service/inline";

  /** Template strings for creating reference maps. */
  public static final String CONNECTION_REF_MAP =
      String.format("#%s %s", CONNECTION_REFERENCE_TAG, "{ :label \"%s\"}");

  public static final String SNAPSHOT_REF_MAP =
      String.format("#%s %s", SNAPSHOT_REFERENCE_TAG, "{ :label \"%s\"}");

  /** The set of keywords used in the connection and snapshot references. */
  private static final Keyword LABEL_KEY = Keyword.intern("label");

  private static final Keyword AS_OF_KEY = Keyword.intern("as-of");
  private static final Keyword SYNC_DB_KEY = Keyword.intern("sync-db");
  private static final Keyword FN_KEY = Keyword.intern("fn");
  private static final Keyword PARAMS_KEY = Keyword.intern("params");

  private static final Keyword DB_BEFORE = Keyword.intern("db-before");
  private static final Keyword DB_AFTER = Keyword.intern("db-after");

  /**
   * Verify if the label is correct.
   *
   * @param l the maybe label.
   * @return Returns the label.
   */
  private static String verifyLabel(String referenceType, Object l) {
    if (!(l instanceof String)) {
      // No matter what, if the label is empty, then this should fail!
      throw new ReferenceException(referenceType, "label is not a string");
    } else if (((String) l).isEmpty()) {
      throw new ReferenceException(referenceType, "Label can not be empty.");
    }
    return (String) l;
  }

  private static Long verifyAsOf(String referenceType, Object l) {
    if (l == null) {
      return null;
    } else if (!(l instanceof Long)) {
      throw new ReferenceException(referenceType, "as-of parameter is not a long");
    }
    return (Long) l;
  }

  private static Boolean verifySyncDb(String referenceType, Object b) {
    if (b == null) {
      return false;
    }

    if (!(b instanceof Boolean)) {
      throw new ReferenceException(referenceType, "syncDb is not a boolean");
    }

    return (Boolean) b;
  }

  /**
   * Create a reference (either Snapshot or Connection) for the input parameter.
   *
   * @param referenceTag The reference tag from which the reference is created from.
   * @param param The parameter to resolve.
   * @return Returns the connection reference.
   */
  private static Reference createReference(
      String referenceTag, Object param, String tenant, String category) {
    String label;
    Long asOf = null;
    boolean syncDb = false;

    if (param == null) {
      throw new ReferenceException(referenceTag, "nil parameter");
    } else if (param instanceof Map) {
      // #eva.client.service/connection-ref/snapshot-ref {
      //   :label {{label}}               ; required
      //   :as-of {{asof}}                ; only for snapshot-ref (optional)
      //   :sync-db {{syncDb}}            ; only for snapshot-ref (optional)
      // }
      Map map = (Map) param;
      label = verifyLabel(referenceTag, map.get(LABEL_KEY));
      asOf = verifyAsOf(referenceTag, map.get(AS_OF_KEY));
      syncDb = verifySyncDb(referenceTag, map.get(SYNC_DB_KEY));
    } else {
      throw new ReferenceException(referenceTag, "Unsupported reference type, must be a Map.");
    }

    Reference ref = null;
    if (CONNECTION_REFERENCE_TAG.equals(referenceTag)) {
      ref = new ConnectionReference(tenant, category, label);
    } else if (SNAPSHOT_REFERENCE_TAG.equals(referenceTag)) {
      ref = new SnapshotReference(tenant, category, label, asOf, syncDb);
    } else {
      throw new ReferenceException(referenceTag, "Unsupported reference type.");
    }

    return ref;
  }

  /**
   * Create a connection reference for the input parameter.
   *
   * @param param The parameter to resolve.
   * @param tenant The tenant.
   * @param category The category.
   * @return Returns the connection reference.
   */
  private static ConnectionReference createConnectionReference(
      Object param, String tenant, String category) {
    return (ConnectionReference) createReference(CONNECTION_REFERENCE_TAG, param, tenant, category);
  }

  /**
   * Create a snapshot reference for the input parameter.
   *
   * @param param The parameter to resolve.
   * @return Returns the snapshot reference.
   */
  private static SnapshotReference createSnapshotReference(
      Object param, String tenant, String category) {
    return (SnapshotReference) createReference(SNAPSHOT_REFERENCE_TAG, param, tenant, category);
  }

  /**
   * Create an inline function from the parameter.
   *
   * @param param The parameter to resolve.
   * @param repo The repo to read from.
   * @return Returns the function value.
   */
  private static Object createInlineFunction(
      RequestContext parentCtx, Object param, final PeerRepository repo) {
    try (RequestContext ctx = parentCtx.startSpan("createInlineFunction")) {
      Function<Object, Symbol> fnVerify =
          (l) -> {
            if (l == null) {
              throw new FunctionInvokeException(INLINE_FUNC_TAG, "fn parameter is nil");
            } else if (!(l instanceof Symbol)) {
              throw new FunctionInvokeException(INLINE_FUNC_TAG, "fn parameter is not a symbol");
            }
            return (Symbol) l;
          };

      Function<Object, List> paramsVerify =
          (l) -> {
            if (l == null) {
              throw new FunctionInvokeException(INLINE_FUNC_TAG, "params parameter is nil");
            } else if (!(l instanceof List)) {
              throw new FunctionInvokeException(INLINE_FUNC_TAG, "params parameter is not a list");
            }
            return (List) l;
          };

      Symbol fn;
      List params;

      if (param == null) {
        throw new FunctionInvokeException(INLINE_FUNC_TAG, "nil inline function parameter");
      } else if (param instanceof Map) {

        /*
        #eva.client.service/inline {
          :fn     {{func}}          ; required
          :params [ {{params...}} ] ; required
        }
        */
        Map map = ((Map) param);
        fn = fnVerify.apply(map.get(FN_KEY));
        params = paramsVerify.apply(map.get(PARAMS_KEY));
      } else {
        throw new FunctionInvokeException(INLINE_FUNC_TAG, "Unsupported snapshot reference type.");
      }

      BiFunction func = getFunction(parentCtx, fn);
      if (func == null) {
        throw new FunctionInvokeException(INLINE_FUNC_TAG, "Unknown function: " + fn.toString());
      }

      PersistentVector l = (PersistentVector) params;
      l = l.cons(parentCtx);
      return func.apply(repo, l);
    }
  }

  /**
   * Get the functions from the internals.
   *
   * @param function The function to get.
   * @return Returns the function to use.
   */
  private static BiFunction getFunction(RequestContext parentCtx, Symbol function)
      throws ClientServiceException {
    try (RequestContext ctx = parentCtx.startSpan("getFunction")) {
      if (functionCache == null) {
        functionCache = new HashMap<>();

        LOGGER.info("Loading functions.");

        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(PeerFunctionCollection.class));

        scanner
            .findCandidateComponents(PeerFunctionCollection.class.getPackage().getName())
            .forEach(
                (def) -> {
                  LOGGER.info("Examining: {}", def.getBeanClassName());
                  try {
                    Class<?> cls =
                        Objects.requireNonNull(ClassUtils.getDefaultClassLoader())
                            .loadClass(def.getBeanClassName());

                    Method[] methods = cls.getMethods();
                    for (Method method : methods) {
                      if (method.isAnnotationPresent(PeerFunction.class)) {

                        PeerFunction func = method.getAnnotation(PeerFunction.class);
                        final Symbol symbol = Symbol.intern(func.value());

                        if (!Modifier.isStatic(method.getModifiers())) {
                          throw new ClientServiceException(
                              HttpStatus.INTERNAL_SERVER_ERROR,
                              "Expected the method '%s' to be static.",
                              symbol.getName());
                        }

                        if (method.getParameterCount() == 0) {
                          throw new ClientServiceException(
                              HttpStatus.INTERNAL_SERVER_ERROR,
                              "Expected the method '%s' to have at least one parameter",
                              symbol.getName());
                        } else {
                          Parameter param = method.getParameters()[0];
                          if (!(PeerRepository.class).isAssignableFrom(param.getType())) {
                            throw new ClientServiceException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Expected the method '%s's first parameter "
                                    + "to be a PeerRepository.",
                                symbol.getName());
                          }
                        }

                        LOGGER.info("Found Function: {}", symbol.getName());
                        BiFunction<PeerRepository, List, Object> funcImpl =
                            ((peerRepository, list) -> {
                              List<Object> params = new ArrayList<>();
                              params.add(peerRepository);
                              params.addAll(list);

                              LOGGER.info(
                                  "Executing '{}:{}'", def.getBeanClassName(), method.getName());

                              Object value;
                              try {
                                value = method.invoke(null, params.toArray());
                              } catch (EvaException ex) {
                                ExceptionUtils.logEvaRelatedException(
                                    ctx,
                                    String.format("Error executing function: {}", symbol.getName()),
                                    ex);
                                throw ex;
                              } catch (Exception e) {
                                ExceptionUtils.logGenericException(
                                    ctx,
                                    String.format("Error executing function: %s", symbol.getName()),
                                    e);
                                throw new ClientServiceException(
                                    e, "Error executing function: %s", symbol.getName());
                              }
                              return value;
                            });

                        if (functionCache.containsKey(symbol)) {
                          throw new ClientServiceException(
                              HttpStatus.INTERNAL_SERVER_ERROR,
                              "Duplicate function: %s",
                              symbol.getName());
                        }
                        functionCache.putIfAbsent(symbol, funcImpl);
                      }
                    }
                  } catch (ClassNotFoundException e) {
                    throw new ClientServiceException(
                        e, "Exception occurred when attempting to initialize Inline Functions");
                  }
                });
      }
      return functionCache.get(function);
    }
  }

  /** Deserialize EDN with support for eva-client-service tags. */
  public static Object deserializeEdn(
      RequestContext parentCtx,
      final PeerRepository repo,
      final String tenant,
      final String category,
      final String edn)
      throws ClientServiceException {
    // Define the collection of deserializers to run on the edn.
    Map<String, Function> deserializers =
        new HashMap<String, Function>() {
          {
            put(
                CONNECTION_REFERENCE_TAG,
                (param) -> createConnectionReference(param, tenant, category));
            put(
                SNAPSHOT_REFERENCE_TAG,
                (param) -> createSnapshotReference(param, tenant, category));
            put(INLINE_FUNC_TAG, (param) -> createInlineFunction(parentCtx, param, repo));
          }
        };
    return SerializerUtils.deserialize(parentCtx.getHttpHeaders(), edn, deserializers);
  }

  /**
   * Given a ConnectionReference string, deserializeEdn a ConnectionReference.
   *
   * @param tenant The tenant of the the reference.
   * @param category The reference in String form.
   * @param label The reference in String form.
   * @return a ConnectionReference.
   */
  public static ConnectionReference createConnectionReferenceFromString(
      RequestContext parentCtx, final String tenant, final String category, final String label)
      throws ClientServiceException {
    try (RequestContext ctx = parentCtx.startSpan("createConnectionReferenceFromString")) {
      Map<String, Function> deserializers =
          new HashMap<String, Function>() {
            {
              put(
                  CONNECTION_REFERENCE_TAG,
                  (param) -> createConnectionReference(param, tenant, category));
            }
          };
      return SerializerUtils.deserialize(ctx.getHttpHeaders(), label, deserializers);
    }
  }

  /**
   * Replaces the #DB tags with a SnapshotReference string. #DB tag replaced with SnapshotReference
   * strings.
   *
   * @param responseMap The response from a transaction.
   * @param connRef The ConnectionReference used for the transaction.
   */
  public static void replaceDbWithSnapshotReference(
      RequestContext parentCtx, Map responseMap, ConnectionReference connRef) {
    try (RequestContext ctx = parentCtx.startSpan("replaceDbWithSnapshotReference")) {
      BiFunction<Object, Object, Object> replaceDBTag =
          (k, v) -> {
            Database db = (Database) v;
            return new SnapshotReference(
                connRef.tenant(), connRef.category(), connRef.label(), false, db);
          };
      responseMap.computeIfPresent(DB_BEFORE, replaceDBTag);
      responseMap.computeIfPresent(DB_AFTER, replaceDBTag);
    }
  }

  public static void countSnapshotAndConnectionReferences(
      Object[] queryParams, int[] referenceCounters) {
    for (Object obj : queryParams) {
      if (obj instanceof Database) {
        referenceCounters[0]++;
      } else if (obj instanceof Connection) {
        referenceCounters[1]++;
      }
    }
  }
}
