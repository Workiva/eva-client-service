# ![](docs/EVA-50x50.png) Eva Client Service [![codecov](https://codecov.workiva.net/gh/Workiva/eva-client-service/branch/master/graph/badge.svg)](https://codecov.workiva.net/gh/Workiva/eva-client-service)

The Eva client service is an Eva peer which exposes Eva functionality to other languages through a _REST_ interface. This model allows developers to work with EVA in non-JVM languages, removes the need to deploy a peer cluster and uncouple their database from their applications. The service also allows for multi-database queries, cross database referencing and prevents multiple round trips for these types of calls. Furthermore, only one service will need to be scaled, and the interface is standard so multiple services can utilize the same client service.

<!-- toc -->

- [Getting Started](#getting-started)
  * [Running with the Eva Catalog Service](#running-with-the-eva-catalog-service)
  * [Running with Persistent H2 Database File](#running-with-persistent-h2-database-file)
  * [Supported Docker Environment Variables](#supported-docker-environment-variables)
- [API Documentation](#api-documentation)
  * [API Versions](#api-versions)
    + [GET /eva/versions](#get-evaversions)
- [API Basics and Overview](#api-basics-and-overview)
  * [Terminology](#terminology)
  * [EDN Content-Type](#edn-content-type)
  * [Correlation Id](#correlation-id)
  * [API Parameters](#api-parameters)
    + [Connection Reference](#connection-reference)
    + [Snapshot Reference](#snapshot-reference)
  * [Inline Functions](#inline-functions)

<!-- tocstop -->

## Getting Started

To run the client service with default settings and an in-memory database, simply pull the latest docker image from drydock:

```bash
docker run --name eva-client-service -p 8080:8080 -d workiva/eva-client-service:latest-release
```

If you are developing on the client service, execute the following to both build and run the application:

```bash
make run-local
```

### Running with the Eva Catalog Service

There may be instances where running the client-service locally connected to the catalog may be useful or required.  To do so, run the following commands which first spins up the catalog locally, pointing at a [simple catalog configuration EDN file](./catalog-configs/inmem.edn) that defines an in-memory database.  Next it spins up the client-service, pointing to the local catalog service on it's default port.

```bash
docker run --name eva-catalog \
  -p 3000:3000 \
  -v $PWD/catalog-configs:/shared \
  -e EVA_CATALOG_DATA=/shared/inmem.edn \
  -d workiva/eva-catalog:latest-release
make catalog-local
```

### Running with Persistent H2 Database File

In order to make use of a persistent h2 database file, we will modify our catalog configuration to include a tenant/category/label combination which corresponds to the `database-id` and `partition-id` used for the h2 file's databases.  [Eva's documentation on this feature can be found here](https://github.com/Workiva/eva/blob/master/docs/local-v2-h2.md) and used to create an initial h2 file.  The h2 file must exist at the provided file-path.

```bash
docker run --name eva-catalog \
  -p 3000:3000 \
  -v $PWD/catalog-configs:/shared \
  -e EVA_CATALOG_DATA=/shared/h2.edn \
  -d Workiva/eva-catalog:latest-release
make catalog-local
```

> Take a look at the included [example catalog config](./catalog-configs/h2.edn) to see how this fits together.

### Supported Docker Environment Variables

| Name                     | Description                                    | Default
| -----------------------  | ------                                         | --
| `CLIENT_SERVICE_PORT`    | The port the client service will listen on     | 8080
| `CATALOG_URL`            | Full URL that points to the eva-catalog service.  `LOCAL` will omit the use of the catalog and use an in-mem database | LOCAL
| `TRACE_RECORDER`         | Name of the tracing recorder to use.  Accepted values are (`harbour` / `jaeger` / `debug` / `off`) | `harbour`
| `DISABLE_TELEMETRY`      | Can be used to disable telemetry.  Acceptable values are (`true` / `false`) | true 
| `LOGBACK_LOG_LEVEL`      | Log level to use. [Valid log levels](https://www.slf4j.org/api/org/apache/log4j/Level.html) | INFO
| `LOGBACK_APPENDER`       | Where to output the logs to (`SOCKET` / `STDOUT`)           | `STDOUT`
| `SANITIZE_EXCEPTIONS`    | Determines whether or not non `ClientServiceExceptions` are sanitized | true
| `YOURKIT_AGENT_ENABLE`   | Enable [YourKit](https://www.yourkit.com/) profiling.  You will need to additionally expose the yourkit port if running locally. | false
| `YOURKIT_AGENT_PATH`     | Path to the YourKit agent | /opt/yourkit-agent/linux-x86-64/libyjpagent.so
| `YOURKIT_AGENT_PORT`     | The egress/ingress port used by YourKit | 10001
| `EX_CAUSE_NEST_LIMIT`    | How many causes on an exception will the client-service recursively traverse and return | 1

## API Documentation

First, a general understanding of [Eva](http://github.com/Workiva/eva) is highly recommended. This project is intended to be a conduit to Eva, and allows for some interesting behaviours that the native Eva peers could find challenging.

It is highly recommended to utilize a program such as [Postman](https://www.getpostman.com/) when iterating on requests to the client service.  For steps to setup Postman and the included example requests [click here](postman/README.md)

**For all examples it will be assumed that the client service is running locally on port 8080**

### API Versions

For future compatibility, this api is versioned to enable schema and endpoint changes in potential subsequent versions.

- [Version 1](docs/v1)

#### GET /eva/versions

This endpoint is used to get a list of the service's supported Eva REST versions, and can be used to check if the calling library is compatible with the client serivce.

##### Example Request

* [GET /eva/versions](http://localhost:8080/eva/versions)

##### Example Response

```clj
["v.1"]
```

## API Basics and Overview

### Terminology

| Term          | Definition
| ------------- | -----------
| `Peer`        | A peer is a client to the eva system which can perform reads and queries.
| `Transactor`  | The transactor is the single write node in the eva ecosystem.
| `Connection`  | The connection describes the communication paths to the peer and transactor.
| `Partition`   | The partition describes the physical storage location and setup.
| `Database`    | Synonymous with `Snapshot`.
| `Snapshot`    | A snapshot (or `Database`) describes the state of the storage at a particular point in time. Note that this point in time may be the `Head` of the `Partition`.
| `Head`        | The most current state within Eva.
| [Catalog Service Configuration Map](https://github.com/Workiva/eva-catalog) | A map of three keys used to connect with Eva, consists of a `Tenant`, `Category`, and `Label`
| `Tenant`      | The tenant for a configuration is a partitioning of ownership for configurations.
| `Category`    | The category for a configuration indicates a cross-configuration coupling.
| `Label`       | The label on a configuration is a name that, along with the tenant and category uniquely identifies a configuration for a specific database.

### EDN Content-Type

To make this service interact with Eva to the highest extent, all calls, including meta data queries or service functionality, handle [EDN](https://github.com/edn-format/edn) as their `Content-Type`. Responses are returned in EDN as well.

### Correlation Id

An optional header `_cid` with a string content is used to correlate between services. If this header is not specified then a generated UUID will be used.

### API Parameters

Unless specified, parameters are all sent through the standard `application/x-www-form-urlencoded` mechanism and require the proper encoding.

The supported `Accept` type is `application/vnd.eva+edn` which is not a standard `MIME` type, but a vendor specified one for Eva with the EDN format.

#### Connection Reference

A Connection reference is a connection to the Eva system in general. This is the reference that would be used when performing a transaction, or invoking a function that requires a connection reference.  Connection references can be defined with a `map` and is distinguished by the EDN tag `#eva.client.service/connection-ref` which precedes the data structure.

| Field       | Map Key     | Vector Index | Type                    | Required?
| ----------- | -------     | ------------ | ----------------------- | ---------
| Label       | `:label`    | 0            | String                  | Yes      

##### Example

Assume we have a database that is identified through:
* Tenant - `test-tenant` - **Defined Through URL Parameter**
* Category - `test-category` - **Defined Through URL Parameter**
* Label - `test-label`

The reference would look like:

```clj
#eva.client.service/connection-ref {
    :label "test-label"
}
```

#### Snapshot Reference

A Snapshot reference is the state of the Eva database at a point in time. This is the reference that would be used when doing a query, a pull, or invoking a function that required a database reference.  Snapshot references is distinguished by the EDN tag `#eva.client.service/snapshot-ref` which precedes the data structure.

Snapshot references are very similar in definition to a connection reference with the exception that they accept an optional `:as-of` field.  The `:as-of` field accepts a `transaction number` or a function which returns one.  This allows for database operations to be performed on the database at that particular point in the transaction history.

| Field       | Map Key     | Type                       | Required?  
| ----------- | -------     | -----------------------    | ---------
| Label       | `:label`    | String                     | Yes      
| As-Of       | `:as-of`    | Long _or_ Inline Function  | No       

##### Examples

Assume we have a database that is identified through:
* Tenant - `test-tenant` - **Defined Through URL Parameter**
* Category - `test-category` - **Defined Through URL Parameter**
* Label - `test-label`

##### Example - Retrieve the most recent snapshot, no need to use `:as-of`

The reference would look like:

```clj
#eva.client.service/snapshot-ref {
    :label "test-label"
}
```

##### Example - Use of `:as-of` with an explicit transaction number

The reference would look like:

```clj
#eva.client.service/snapshot-ref {
    :label "test-label"
    :as-of 5
}
```

##### Example - Use of `:as-of` by retrieving a transaction number through an inline function

If there is a reference to the snapshot to use, inline functions can be used to
calculate the appropriate snapshot 'T' or the transaction number. For more information about the
[inline functions, see below.](#inline-functions)

```clj
#eva.client.service/snapshot-ref {
  :label "{{label}}"
  :as-of #eva.client.service/inline {
    :fn first
    :params #eva.client.service/inline {
      :fn query
      :params [
        "[:find ?tx :in $ ?t :where [?b :book/title ?t ?tx]]"
        [#eva.client.service/snapshot-ref ["{{label}}"]
        "\"First Book\""]
      ]
    }
}}
```

> Here we use two inline-functions chained together to get our result.  First we use the `query` inline function to get the `transaction id` of the book titled `"First Book"`.  This is the value we need for `:as-of`, however the query could return multiple results so it is returned as a list, the `first` inline function is used to get the first element.  Either `transaction ids` or `transaction numbers` are valid inputs for `:as-of`.  This syntax can easily get out of control, unless this is a frequently used query and you want to minimize network calls, consider breaking it out into two queries.

### Inline Functions

Some snapshots are not known directly at the time of execution, but can be resolved through a function. This is the way to pass these functions into the Eva reference structures.  Inline functions are preceded with the tag `#eva.client.service/inline`.

It is worth mentioning that if the inline function requires a reference as one of it's parameters that the tenant and category will be inferred to be the same as those originally provided in the request URL.


| Field       | Map Key   | Type     | Required? | Description
| ----------- | --------- | -------- | --------- | ------------
| Function    | `:fn`     | String   | Yes       | Name of one of the [provided functions](#inline-function-documentation) or [a custom extended function](#extending-the-set-of-functions)
| Parameters  | `:params` | Vector   | No        | List of required arguments for the intended function

##### Template Example

Representation:

```clj
#eva.client.service/inline {
  :fn     {Function}
  :params [ {Parameters} ]
}
```

Form more details, see the [inline functions documentation](docs/v1/inline-functions.md). To create new inline functions, see the [creating inline function documentation](docs/v1/creating-inline-fn.md)
