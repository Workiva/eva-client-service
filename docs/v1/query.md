# Query

<!-- toc -->

- [Description](#description)
- [POST /eva/v.1/q/{tenant}/{category}](#post-evav1qtenantcategory)
  * [Supported Form Parameters](#supported-form-parameters)
  * [Example](#example)
- [Further Reading](#further-reading)

<!-- tocstop -->

## Description

Executes a query against inputs. Query parse results are cached. Returns a data structure based on the find specification passed in.

Inputs are data sources, for example:
- A [Snapshot Reference](../../README.md#snapshot-reference)
- An arbitrary value, such as a string
- A list of lists
- [Rules](https://github.com/Workiva/eva/blob/master/docs/eva_101.md#rules)

These inputs are described within the `:in` clause.  If only one data source is provided (the required `snapshot reference`) then the `:in` clause is not required.

A query can be represented as a map, a list, or a string:

`map` representation:

```clj
{:find vars-and-aggregates
 :with vars-included-but-not-returned
 :in sources
 :where where-clauses}
```

> `vars`, `sources` and `where-clauses` are `lists`

> `:with` is an optional clause and instead names `vars` to be kept in the aggregation set but not returned in the final result.

`list` representation:

```clj
[:find ?var1 ?var2 ...
 :with ?var3 ...
 :in $src1 $src2 ...
 :where clause1 clause2 ...]
```
                         
 > A query represented in list form is simply converted into a map internally.

 A query represented as a `string` would constitute a valid EDN string that when read resulted in either the `map` or `list` representation.

## POST /eva/v.1/q/{tenant}/{category}

### Supported Form Parameters

| Key           | Required? | Value Description
| ------------- | --------- | ------------------
| `query`       |  Yes      | The EDN query.
| `p[0-n]`      |  No       | The parameters required to execute the query. Note that there are multiple fields indexed in order from 0.

> Typically the first parameter (`p[0]`) will always be a `snapshot reference`.

### Example

This is a simple query to get all of the `:db/ident` values in the database.  This should work regardless if there is even a schema transacted.

#### Form Parameter Values

`query` Value:

```clj
[:find ?attr 
 :in
 :where [_ :db/ident ?attr]]
```

`p[0]` Value:

```clj
#eva.client.service/snapshot-ref {
    :label "test-label"
}
```

#### cURL Command

```bash
curl -X POST \
  http://localhost:8080/eva/v.1/q/test-tenant/test-category \
  -H 'Accept: application/vnd.eva+edn' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'query=%5B%3Afind+%3Fattr+%3Ain+%24+%3Awhere+%5B_+%3Adb%2Fident+%3Fattr%5D%5D&p%5B0%5D=%23eva.client.service%2Fsnapshot-ref+%7B%0A++++%3Alabel+%22test-label%22%0A%7D'
```

#### Example Result

```clj
[[:db.type/instant] [:db/fulltext] [:db.type/string] [:db.type/uri] [:db.type/ref] [:db/ident] [:db/noHistory] [:db.type/fn] [:db/index] [:db.cardinality/many] [:db.type/double] [:db/cardinality] [:db.install/valueType] [:db.type/boolean] [:db/unique] [:db.install/attribute] [:db.fn/cas] [:db.type/bigint] [:db/retract] [:db/valueType] [:db.part/db] [:db.type/bytes] [:db.unique/value] [:db.part/tx] [:db.unique/identity] [:db.type/float] [:db/isComponent] [:db.part/user] [:db/add] [:db.fn/retractEntity] [:db.type/long] [:db.type/uuid] [:db.cardinality/one] [:db/txInstant] [:db/doc] [:db.install/partition] [:db.type/keyword] [:db.type/bigdec] [:db/fn]]
```

## Further Reading

- [EVA 101 - Sample Queries, Rules, etc](https://github.com/Workiva/eva/blob/master/docs/eva_101.md)
