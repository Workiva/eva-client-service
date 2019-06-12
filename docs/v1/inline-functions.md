# Inline functions

The following functions are currently supported:

| Function      | Parameters | Description
|----------     |------------|------------
| `entid` | `Snapshot Reference`, `ident`  | Returns the entity id associated with a symbolic keyword, or the id itself if passed.
| `latestT` | `Connection Reference` | Returns the latest tx-num that the Connection has updated its local state to match.
| `query` | `query`, `inputs` | Returns the result of a query.
| `first` | `collection` | Returns the first element in the collection.

If there is a need for a new inline function, add it according to the [creating inline functions](creating-inline-fn.md) documentation.

### `entid` Example

```clj
#eva.client.service/inline {
    :fn entid
    :params [
        #eva.client.service/snapshot-ref ["test-label"]
        [:something/tx-metadata "hello"]
    ]
}
```

### `latestT` Example

```clj
#eva.client.service/inline {
    :fn latestT
    :params [
        #eva.client.service/connection-ref { :label "test-label" } 
        :martin/tx-metadata "hello"]]
}
```

### `query` Example

```clj
#eva.client.service/inline {
    :fn query
    :params [
        "[:find ?tx :in $ ?t :where [?b :book/title ?t ?tx]]"
        [
            #eva.client.service/snapshot-ref [
                "{{label}}"]
            "\"First Book\""
        ]
    ]
}
```

### `first` Example

```clj
#eva.client.service/inline { :fn first :params [1 2 3] }
```
