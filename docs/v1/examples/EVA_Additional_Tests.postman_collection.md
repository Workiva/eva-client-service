# EVA-Client-Service - Additional Tests

<!-- toc -->

- [Basic ident Call for :db/ident - Non-Strict](#basic-ident-call-for-dbident---non-strict)
- [Basic ident Call for :db/ident - Strict](#basic-ident-call-for-dbident---strict)
- [Basic ident Call for Non-Existant ident - Non-Strict](#basic-ident-call-for-non-existant-ident---non-strict)
- [Basic ident Call for Non-Existant ident - Strict](#basic-ident-call-for-non-existant-ident---strict)
- [Basic attribute Call for :db/doc - Non-Strict](#basic-attribute-call-for-dbdoc---non-strict)
- [Basic attribute Call for :db/doc - Strict](#basic-attribute-call-for-dbdoc---strict)
- [Basic attribute Call for Non-Existant attribute - Non-Strict](#basic-attribute-call-for-non-existant-attribute---non-strict)
- [Basic attribute Call for Non-Existant attribute - Strict](#basic-attribute-call-for-non-existant-attribute---strict)
- [Basic extant-entity Call for :db/doc](#basic-extant-entity-call-for-dbdoc)
- [Basic extant-entity Call for Non-Existant ident](#basic-extant-entity-call-for-non-existant-ident)
- [API Version List](#api-version-list)
- [Health](#health)
- [LatestT](#latestt)
- [Transact Schema - In-Memory 1](#transact-schema---in-memory-1)
- [Make Second DB - Should have no Schema](#make-second-db---should-have-no-schema)
- [With (simulated transaction) Schema - In-Memory 2](#with-simulated-transaction-schema---in-memory-2)
- [Check Schema on second DB](#check-schema-on-second-db)
- [Entity](#entity)
- [Inline Function - ffirst](#inline-function---ffirst)
- [Query with SyncDb](#query-with-syncdb)

<!-- tocstop -->

## Basic ident Call for :db/ident - Non-Strict

### Description

Call the `ident` function on Entity-ID `3` which corresponds to `:db/ident`


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### entid - _text_

```
3
```

## Basic ident Call for :db/ident - Strict

### Description

Call the `ident` function on Entity-ID `3` which corresponds to `:db/ident`. If nothing is found, an error will be thrown rather than returning `nil`


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### entid - _text_

```
3
```

#### strict - _text_

```
true
```

## Basic ident Call for Non-Existant ident - Non-Strict

### Description

Call `ident` on Entity ID `-1` which does not correspond to anything.  `nil` will be returned.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### entid - _text_

```
-1
```

## Basic ident Call for Non-Existant ident - Strict

### Description

Call `ident` on Entity ID `-1` which does not correspond to anything.  An exception will be thrown.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### entid - _text_

```
-1
```

#### strict - _text_

```
true
```

## Basic attribute Call for :db/doc - Non-Strict

### Description

Call `attribute` with attribute ID `:db/doc`


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### attrId - _text_

```
:db/doc
```

## Basic attribute Call for :db/doc - Strict

### Description

Call `attribute` with attribute ID `:db/doc`.  If nothing is found, an error will be thrown.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### attrId - _text_

```
:db/doc
```

#### strict - _text_

```
true
```

## Basic attribute Call for Non-Existant attribute - Non-Strict

### Description

Call `attribute` with attribute ID `:db/nope` which does not correspond got anything. `nil` will be returned.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### attrId - _text_

```
:db/nope
```

## Basic attribute Call for Non-Existant attribute - Strict

### Description

Call `attribute` with attribute ID `:db/nope` which does not correspond got anything. An error will be thrown.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### attrId - _text_

```
:db/nope
```

#### strict - _text_

```
true
```

## Basic extant-entity Call for :db/doc

### Description

Check if an entity exists in the database that uses the `:db/doc` identifier.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### ident - _text_

```
:db/doc
```

## Basic extant-entity Call for Non-Existant ident

### Description

Check if an entity exists in the database that uses the `:db/nope` identifier, which there is not one.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/snapshot-ref {
   :label "test-label"
}
```

#### ident - _text_

```
:db/nope
```

## API Version List

### Description

Get a list of the supported versions of the client-service API.


### Method - **GET**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |

## Health

### Description

Calls the Status Endpoint


### Method - **GET**

## LatestT

### Description

Calls `latestT` Deprecated.


### Method - **GET**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |

## Transact Schema - In-Memory 1

### Description

The response of a successful transact is a map with four keys, :db-before, :db-after, :tempids, and :tx-data. :db-before and :db-after contains snapshots of the database before and after the transaction. :tempids contains a mapping of temporary ids that occurred as part of the transaction to their corresponding permanent ids. :tx-data contains the individual #datom vectors that were inserted into the database.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/connection-ref {
  :label "test-label"
}
```

#### transaction - _text_

```
[
{:db/id #db/id [:db.part/user]
 :db/ident :book/title
 :db/doc "Title of a book"
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}

{:db/id #db/id [:db.part/user]
 :db/ident :book/year_published
 :db/doc "Date book was published"
 :db/valueType :db.type/long
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}

{:db/id #db/id [:db.part/user]
 :db/ident :book/author
 :db/doc "Author of a book"
 :db/valueType :db.type/ref
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}

{:db/id #db/id [:db.part/user]
 :db/ident :author/name
 :db/doc "Name of author"
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}
 ]
```

## Make Second DB - Should have no Schema


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### query - _text_

```
[:find ?attr
 :where [_ :db/ident ?attr]]
```

#### p[0] - _text_

```
#eva.client.service/snapshot-ref {
  :label "test-label-2"
}
```

## With (simulated transaction) Schema - In-Memory 2

### Description

The response of a successful `with` is a map with the same four keys as transact.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |


### Body Fields - **urlencoded**

#### reference - _text_

```
#eva.client.service/connection-ref {
  :label "test-label-2"
}
```

#### transaction - _text_

```
[
{:db/id #db/id [:db.part/user]
 :db/ident :book/title
 :db/doc "Title of a book"
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}

{:db/id #db/id [:db.part/user]
 :db/ident :book/year_published
 :db/doc "Date book was published"
 :db/valueType :db.type/long
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}

{:db/id #db/id [:db.part/user]
 :db/ident :book/author
 :db/doc "Author of a book"
 :db/valueType :db.type/ref
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}

{:db/id #db/id [:db.part/user]
 :db/ident :author/name
 :db/doc "Name of author"
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one
 :db.install/_attribute :db.part/db}
 ]
```

## Check Schema on second DB

### Description

Check schema after `with` call to ensure tx-data was not durably persisted.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### query - _text_

```
[:find ?attr
 :where [_ :db/ident ?attr]]
```

#### p[0] - _text_

```
#eva.client.service/snapshot-ref {
  :label "test-label-2"
}
```

## Entity

### Description

Next is the query, [:find ?b :where [?b :book/title "First Book"]]. There is a lot to take in here so we'll break it down piece-by-piece. First of all, every query you write needs to be wrapped in a vector ([...]). The query starts with the :find keyword followed by a number of logic variables (lvar for short) denoted with a ?. The :where clause follows and, similarly to SQL, is used to restrict the query results.

The tuple [?b :book/title "First Book"] is called a data pattern. All querying is essentially matching that pattern to the datom 5-tuple we discussed earlier ([eid attr val tx added?]). In this case we are asking for all of the entity ids (?b) which have the attribute :book/title with value "First Book". What about tx and added?, why don't they appear in the clause? Simply, if not present they are replaced with implicit blanks. Expanding the tuple to its full form would yield, [?b :book/title "First Book" _ _]. We'll talk more about blanks later.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### query - _text_

```
[:find ?ident
 :in $ ?my-ident
 :where [?my-ident :db/ident ?ident]]
```

#### p[0] - _text_

```
#eva.client.service/snapshot-ref {
  :label "test-label"
}
```

#### p[1] - _text_

```
#eva.client.service/inline { 
  :fn ident
  :params [
    #eva.client.service/snapshot-ref {
      :label "test-label"
    }
    1
  ]
}
```

## Inline Function - ffirst

### Description

ffirst inline function gets the first item of the first item of a collection.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### query - _text_

```
[:find ?ident
 :in $ ?my-ident
 :where [?my-ident :db/ident ?ident]]
```

#### p[0] - _text_

```
#eva.client.service/snapshot-ref {
  :label "test-label"
}
```

#### p[1] - _text_

```
#eva.client.service/inline { 
  :fn ident
  :params [
    #eva.client.service/snapshot-ref {
      :label "test-label"
    }
    1
  ]
}
```

## Query with SyncDb

### Description

Next is the query, [:find ?b :where [?b :book/title "First Book"]]. There is a lot to take in here so we'll break it down piece-by-piece. First of all, every query you write needs to be wrapped in a vector ([...]). The query starts with the :find keyword followed by a number of logic variables (lvar for short) denoted with a ?. The :where clause follows and, similarly to SQL, is used to restrict the query results.

The tuple [?b :book/title "First Book"] is called a data pattern. All querying is essentially matching that pattern to the datom 5-tuple we discussed earlier ([eid attr val tx added?]). In this case we are asking for all of the entity ids (?b) which have the attribute :book/title with value "First Book". What about tx and added?, why don't they appear in the clause? Simply, if not present they are replaced with implicit blanks. Expanding the tuple to its full form would yield, [?b :book/title "First Book" _ _]. We'll talk more about blanks later.


### Method - **POST**


### Headers

| Key | Value |
| --- | ----- |
| Accept | application/vnd.eva+edn |
| Content-Type | application/x-www-form-urlencoded |


### Body Fields - **urlencoded**

#### query - _text_

```
[:find ?b 
 :in $ ?t 
 :where [?b :book/title ?t]]
```

#### p[0] - _text_

```
#eva.client.service/snapshot-ref {
  :label "test-label"
}
```

#### p[1] - _text_

```
"First Book"
```

