# Transact

<!-- toc -->

- [Description](#description)
- [POST /eva/v.1/transact/{tenant}/{category}](#post-evav1transacttenantcategory)
  * [Supported Form Parameters](#supported-form-parameters)
  * [Example - Normal Transaction](#example---normal-transaction)
  * [Example - Transaction Function](#example---transaction-function)
- [Further Reading](#further-reading)

<!-- tocstop -->

## Description

This endpoint writes the requested transaction to the Eva system.  A transaction in Eva is just a collection of data structures.

## POST /eva/v.1/transact/{tenant}/{category}

### Supported Form Parameters

| Key           | Required? | Value Description
| ------------- | --------- | ------------------
| `reference`   |  Yes      | [The connection reference](../../README.md#connection-reference)
| `transaction` |  Yes      | The EDN transaction.

### Example - Normal Transaction

Transacting a simple book-themed schema that is used in the [EVA-101](https://github.com/Workiva/eva/blob/master/docs/eva_101.md#a-schema)

Something to keep in mind when constructing these EDN parameters: `(eva/tempid)` usages should be refactored to use `#db/id [:db.part/user]` or `#db/id [:db.part/user <Negative Integer>]` if referencing the same temp-id within the same transaction is required. 

#### Form Parameter Values

`reference` Value:

```clj
#eva.client.service/connection-ref {
    :label "test-label"
}
```

`transaction` Value:

```clj
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

#### cURL Command

```bash
curl -X POST \
  http://localhost:8080/eva/v.1/transact/test-tenant/test-category \
  -H 'Accept: application/vnd.eva+edn' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'reference=%23eva.client.service%2Fconnection-ref+%7B%0A++++%3Alabel+%22test-label%22%0A%7D&transaction=%5B%0A%7B%3Adb%2Fid+%23db%2Fid+%5B%3Adb.part%2Fuser%5D%0A+%3Adb%2Fident+%3Abook%2Ftitle%0A+%3Adb%2Fdoc+%22Title+of+a+book%22%0A+%3Adb%2FvalueType+%3Adb.type%2Fstring%0A+%3Adb%2Fcardinality+%3Adb.cardinality%2Fone%0A+%3Adb.install%2F_attribute+%3Adb.part%2Fdb%7D%0A%0A%7B%3Adb%2Fid+%23db%2Fid+%5B%3Adb.part%2Fuser%5D%0A+%3Adb%2Fident+%3Abook%2Fyear_published%0A+%3Adb%2Fdoc+%22Date+book+was+published%22%0A+%3Adb%2FvalueType+%3Adb.type%2Flong%0A+%3Adb%2Fcardinality+%3Adb.cardinality%2Fone%0A+%3Adb.install%2F_attribute+%3Adb.part%2Fdb%7D%0A%0A%7B%3Adb%2Fid+%23db%2Fid+%5B%3Adb.part%2Fuser%5D%0A+%3Adb%2Fident+%3Abook%2Fauthor%0A+%3Adb%2Fdoc+%22Author+of+a+book%22%0A+%3Adb%2FvalueType+%3Adb.type%2Fref%0A+%3Adb%2Fcardinality+%3Adb.cardinality%2Fone%0A+%3Adb.install%2F_attribute+%3Adb.part%2Fdb%7D%0A%0A%7B%3Adb%2Fid+%23db%2Fid+%5B%3Adb.part%2Fuser%5D%0A+%3Adb%2Fident+%3Aauthor%2Fname%0A+%3Adb%2Fdoc+%22Name+of+author%22%0A+%3Adb%2FvalueType+%3Adb.type%2Fstring%0A+%3Adb%2Fcardinality+%3Adb.cardinality%2Fone%0A+%3Adb.install%2F_attribute+%3Adb.part%2Fdb%7D%0A+%5D'
```

#### Example Result

```clj
{:tempids {
   -9223363240760753599 8796093023232,
   -9223363240760753596 8796093023233,
   -9223363240760753598 8796093023234,
   -9223363240760753597 8796093023235}, 
 :db-before #DB[0], 
 :eva.client.service/tempids {
   #db/id[:db.part/user -1000001] 8796093023232, 
   #db/id[:db.part/user -1000004] 8796093023233, 
   #db/id[:db.part/user -1000002] 8796093023234, 
   #db/id[:db.part/user -1000003] 8796093023235}, 
 :tx-data (
   #datom[8796093023233 5 35 4398046511105 true] 
   #datom[0 20 8796093023233 4398046511105 true] 
   #datom[8796093023232 4 30 4398046511105 true] 
   #datom[8796093023234 3 :book/year_published 4398046511105 true] 
   #datom[8796093023232 9 "Title of a book" 4398046511105 true] 
   #datom[8796093023232 3 :book/title 4398046511105 true] 
   #datom[8796093023234 9 "Date book was published" 4398046511105 true] 
   #datom[8796093023235 5 35 4398046511105 true] 
   #datom[0 20 8796093023235 4398046511105 true] 
   #datom[0 20 8796093023234 4398046511105 true] 
   #datom[8796093023235 4 32 4398046511105 true] 
   #datom[4398046511105 15 #inst "2018-05-01T17:29:22.913-00:00" 4398046511105 true] 
   #datom[8796093023233 9 "Name of author" 4398046511105 true] 
   #datom[8796093023234 4 22 4398046511105 true] 
   #datom[0 20 8796093023232 4398046511105 true] 
   #datom[8796093023234 5 35 4398046511105 true] 
   #datom[8796093023235 9 "Author of a book" 4398046511105 true] 
   #datom[8796093023232 5 35 4398046511105 true] 
   #datom[8796093023233 4 30 4398046511105 true] 
   #datom[8796093023235 3 :book/author 4398046511105 true] 
   #datom[8796093023233 3 :author/name 4398046511105 true]), 
 :db-after #DB[1]}
```

### Example - Transaction Function

Transacting a simple transaction function that increases a particular entity's `:account/balance`, this assumes the [EVA 102's schema](https://github.com/Workiva/eva/blob/master/docs/eva_102.md#schema-and-entities) is transacted already.

Somethings to keep in mind when constructing these EDN parameters: `:code` should not be quoted (preceeded with a `'`) and `(eva/function)` should be replaced with the `#db/fn` EDN tag.

We also go over invoking this function with the client-service [here](invoke.md#example---custom-transaction-function).

#### Form Parameter Values

`reference` Value:

```clj
#eva.client.service/connection-ref {
    :label "test-label"
}
```

`transaction` Value:

```clj
[{:db/id #db/id [:db.part/user -1]
  :db/ident :inc-balance
  :db/doc "Data function that increments value of attribute by an amount."
  :db/fn #db/fn {:lang "clojure"
                 :params [db e amount]
                 :code [[:db/add e :account/balance
                         (-> (d/entity db e) :account/balance (+ amount))]]
                }}]
```

#### cURL Command

```bash
curl -X POST \
  http://localhost:8080/eva/v.1/transact/test-tenant/test-category \
  -H 'Accept: application/vnd.eva+edn' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Postman-Token: a3592815-db12-4b5d-a493-20217f405e65' \
  -H 'cache-control: no-cache' \
  -d 'reference=%23eva.client.service%2Fconnection-ref%20%7B%0A%20%20%20%20%3Alabel%20%22test-label%22%0A%7D&transaction=%5B%7B%3Adb%2Fid%20%23db%2Fid%20%5B%3Adb.part%2Fuser%20-1%5D%0A%20%20%20%20%3Adb%2Fident%20%3Ainc-balance%0A%20%20%20%20%3Adb%2Fdoc%20%22Data%20function%20that%20increments%20value%20of%20attribute%20by%20an%20amount.%22%0A%20%20%20%20%3Adb%2Ffn%20%23db%2Ffn%20%7B%3Alang%20%22clojure%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%3Aparams%20%5Bdb%20e%20amount%5D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%3Acode%20%5B%5B%3Adb%2Fadd%20e%20%3Aaccount%2Fbalance%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20(-%3E%20(d%2Fentity%20db%20e)%20%3Aaccount%2Fbalance%20(%2B%20amount))%5D%5D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%7D%0A%20%20%20%5D'
```

#### Example Result

```clj
{:tempids {-9223363240761753599 8796093023237}, :db-before "#eva.client.service/snapshot-ref [ \"test-label\" 3 ]", :eva.client.service/tempids {#db/id[:db.part/user -1] 8796093023237}, :tx-data (#datom[4398046511108 15 #inst "2018-10-22T20:01:26.415-00:00" 4398046511108 true] #datom[8796093023237 11 #eva.functions.DBFn{:lang clojure, :params [db e amount], :code [[:db/add e :account/balance (-> (d/entity db e) :account/balance (+ amount))]], :imports nil, :requires nil, :fn-name nil} 4398046511108 true] #datom[8796093023237 9 "Data function that increments value of attribute by an amount." 4398046511108 true] #datom[8796093023237 3 :inc-balance 4398046511108 true]), :db-after "#eva.client.service/snapshot-ref [ \"test-label\" 4 ]"}
```

## Further Reading

- [EVA 101 - Several Examples of Transactions](https://github.com/Workiva/eva/blob/master/docs/eva_101.md)
- [EVA 102 - Several Examples of Transaction Functions](https://github.com/Workiva/eva/blob/master/docs/eva_102.md)
