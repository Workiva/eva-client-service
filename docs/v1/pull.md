# Pull

<!-- toc -->

- [Description](#description)
- [POST /eva/v.1/pull/{tenant}/{category}](#post-evav1pulltenantcategory)
  * [Supported Form Parameters](#supported-form-parameters)
  * [Example](#example)
- [Further Reading](#further-reading)

<!-- tocstop -->

## Description

Using the `pull` endpoint, we can replace queries that only retrieve information about a particular entity or entities. Returns a hierarchical selection of attributes for one or more entities.

## POST /eva/v.1/pull/{tenant}/{category}

### Supported Form Parameters

| Key           | Required? | Value Description
| ------------- | --------- | ------------------
| `reference`   |  Yes      | [The snapshot reference](../../README.md#snapshot-reference)
| `ids`         |  Yes      | Either a single entity id or an EDN list of entity ids.
| `pattern`     |  Yes      | The `pattern` is where you can specify which attributes you would like to have returned. Using * as your pattern indicates you want all of the attributes for this particular entity.

### Example

This example assumes that at least a schema has been transacted into the Eva database.  Follow the example in the [transaction documentation](transact.md) to accomplish this.
Transacting a simple book-themed schema that is used in the [EVA-101](https://github.com/Workiva/eva/blob/master/docs/eva_101.md#a-schema)

#### Form Parameter Values

`reference` Value:

```clj
#eva.client.service/connection-ref {
    :label "test-label"
}
```

`ids` Value:

```clj
8796093023235
```

`pattern` Value:

```clj
[*]
```

#### cURL Command

```bash
curl -X POST \
  http://localhost:8080/eva/v.1/pull/test-tenant/test-category \
  -H 'Accept: application/vnd.eva+edn' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'reference=%23eva.client.service%2Fsnapshot-ref+%7B%0A++++%3Alabel+%22test-label%22%0A%7D+&ids=8796093023235&pattern=%5B*%5D'
```

#### Example Result

```clj
{:db/id 8796093023235, 
 :db/ident :book/author, 
 :db/valueType {:db/id 32}, 
 :db/cardinality {:db/id 35}, 
 :db/doc "Author of a book"}
```

## Further Reading

- [EVA 101 - Examples on Pull API](https://github.com/Workiva/eva/blob/master/docs/eva_101.md#how-do-i-get-a-full-entity)
