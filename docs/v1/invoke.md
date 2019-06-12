# Invoke

<!-- toc -->

- [Description](#description)
- [POST /eva/v.1/invoke/{tenant}/{category}](#post-evav1invoketenantcategory)
  * [Supported Form Parameters](#supported-form-parameters)
  * [Example - Compare and Swap](#example---compare-and-swap)
  * [Example - Custom Transaction Function](#example---custom-transaction-function)
- [Further Reading](#further-reading)

<!-- tocstop -->

## Description

Execute a particular function. Note that invoking a function will not actually perform the desired side-effect.  For example, invoking compare and swap (`:db.fn/cas`) will not change the true value of the entity.  Instead, `invoke` will return the required `:db.add`s and `:db.retract`s needed to perform the compare and swap, and these must be transacted separately.

Therefore, it often would make more sense to place the `:db.fn/cas` [inside of a transaction call](#running-`:db.fn/cas`-inside-of-a-transaction).  But there are instances where `invoke` serves useful, as non-transaction functions would return a normal result instead.

## POST /eva/v.1/invoke/{tenant}/{category}

### Supported Form Parameters

| Key           | Required? | Value Description
| ------------- | --------- | ------------------
| `reference`   |  Yes      | [The snapshot reference](../../README.md#snapshot-reference)
| `function`    |  Yes      | The function identifier to call.
| `p[0-n]`      |  No       | The parameters required to execute the function. Note that there are multiple fields indexed in order from 0.

### Example - Compare and Swap

In this example, we will invoke the `:db.fn/cas` function which takes 5 arguments: A db, an entity id, attribute, old value and the new value.  Keep in mind that since we are using invoke the database value is not implicitly passed in as the first argument, so we must explicitly pass it in ourselves.

#### Form Parameter Values

`reference` Value:

```clj
#eva.client.service/snapshot-ref {
    :label "test-label"
}
```

`function` Value:

```clj
:db.fn/cas
```

`p[0]` Value:

```clj
#eva.client.service/snapshot-ref {
    :label "test-label"
}
```

`p[1]` Value:

```clj
0
```

`p[2]` Value:

```clj
:db/doc
```

`p[3]` Value:

```clj
"The default database partition."
```

`p[4]` Value:

```clj
"Testing"
```

#### cURL Command

```bash
curl -X POST \
  http://localhost:8080/eva/v.1/invoke/test-tenant/test-category \
  -H 'Accept: application/vnd.eva+edn' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'function=%3Adb.fn%2Fcas&reference=%23eva.client.service%2Fsnapshot-ref+%7B%0A++++%3Alabel+%22test-label%22%0A%7D&p%5B0%5D=%23eva.client.service%2Fsnapshot-ref+%7B%0A++++%3Alabel+%22test-label%22%0A%7D&p%5B1%5D=0&p%5B2%5D=%3Adb%2Fdoc&p%5B3%5D=%22The+default+database+partition.%22&p%5B4%5D=%22Testing%22'
```

#### Example Result

```clj
[
  [:db/retract 0 :db/doc "The default database partition."] 
  [:db/add 0 :db/doc "Testing"]
]
```

##### Running `:db.fn/cas` Inside of a Transaction

The result returned from `:db.fn/cas` would then need to be transacted to change the `:db/doc` value to "Testing".  Instead of using invoke, `:db.fn/cas` could be executed inside of a [transaction](transact.md) with the following transaction value:

```clj
[[:db.fn/cas 
    0
    :db/doc
    "The default database partition."
    "Testing"]]
```

And the result would look something like this:

```clj
{:tempids {}, 
 :db-before #DB[1], 
 :eva.client.service/tempids {}, 
 :tx-data (
   #datom[0 9 "Testing" 4398046511106 true] 
   #datom[4398046511106 15 #inst "2018-05-01T18:21:04.234-00:00" 4398046511106 true] 
   #datom[0 9 "The default database partition." 4398046511106 false]), 
 :db-after #DB[2]}
```

### Example - Custom Transaction Function

In this example, we will invoke the `:inc-balance` function which takes 3 arguments: a db, an entity id and the amount to add onto the existing `:account/balance`.  This function is taken out of the [EVA 102](https://github.com/Workiva/eva/blob/master/docs/eva_102.md#schema-and-entities) and depends on it's schema to work.  Once again, keep in mind that since we are using invoke the database value is not implicitly passed in as the first argument, so we must explicitly pass it in ourselves.  We also go over transacting this function to the client-service [here](transact.md#example---transaction-function).

#### Form Parameter Values

`reference` Value:

```clj
#eva.client.service/snapshot-ref {
    :label "test-label"
}
```

`function` Value:

```clj
:inc-balance
```

`p[0]` Value:

```clj
#eva.client.service/snapshot-ref {
    :label "test-label"
}
```

`p[1]` Value:

```clj
8796093023236
```

`p[2]` Value:

```clj
30
```

#### cURL Command

```bash
curl -X POST \
  http://localhost:8080/eva/v.1/invoke/test-tenant/test-category \
  -H 'Accept: application/vnd.eva+edn' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'function=%3Adb.fn%2Fcas&reference=%23eva.client.service%2Fsnapshot-ref+%7B%0A++++%3Alabel+%22test-label%22%0A%7D&p%5B0%5D=%23eva.client.service%2Fsnapshot-ref+%7B%0A++++%3Alabel+%22test-label%22%0A%7D&p%5B1%5D=0&p%5B2%5D=%3Adb%2Fdoc&p%5B3%5D=%22The+default+database+partition.%22&p%5B4%5D=%22Testing%22'
```

#### Example Result

```clj
[[:db/add 8796093023236 :account/balance 10030]]
```

## Further Reading

- [EVA 101 - Example of using :db.fn/cas in a transaction](https://github.com/Workiva/eva/blob/master/docs/eva_102.md#schema-and-entities)
