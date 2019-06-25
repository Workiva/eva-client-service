# Eva client service JSON formatted EDN - NOT IMPLEMENTED / REFERENCE DOCUMENTATION FOR FUTURE IMPLEMENTATION ONLY

There are cases when EDN is too difficult to process for a language and using an intermediary notation is more suitable.
For these reasons, the eva-client-service can interpret this JSON format and convert it under the hood to EDN.

All endpoints that support the `Accept` header value of `application/vnd.eva+edn` can also support `application/vnd.eva+json`.
Furthermore `Content-Type` header for `application/x-www-form-urlencoded` have a correlated `application/vnd.eva+json`
implementation.

## JSON Envelope

All of the JSON values for EDN are enclosed in an envelope. These envelopes have slightly different `data` fields for
various types of transports.

| `data-type`  | `data` field type | Description |
|--------------|-------------------|-------------|
| `fields`     | map of structured json | This transport is used to pass information like the form data used in the post bodies where various fields may be passed
| `struct`     | [structured json](#general-json-represnetation) | This is the transport used to pass the JSON formatted EDN data
| `edn`        | string            | This is used for transporting the EDN directly


### `fields` transport

This transport is mainly used for any `POST` requests to the service. All field names are as they appear in the
`application/x-www-form-urlencoded` request and the bodies of those fields are JSON formatted EDN. This value can not
be returned via a parameter on the `Accept` header value.

```JSON
{
    "meta": {
        "ver": 1,
        "data-type": "fields"
    },
    "data": {
        "{{field-name}}": {{field-value}}
    }
}
```

### `struct` transport

This is the default return value for any requests with the `Accept` header set to `application/vnd.eva+json`. The data value is
JSON formatted EDN. To explicitly return this set the `Accept` header value to `application/vnd.eva+json;data-type=struct`.

```JSON
{
    "meta": {
        "ver": 1,
        "data-type": "struct"
    },
    "data": {
        {{json-edn}}
    }
}
```

### `edn` transport

This transport mechanism allows for EDN strings to be sent over the wire. In general, this will have limited usage, but if
no other mechanism is available then this may be used. If you would like to get this response then the `Accept` header value
must be `application/vnd.eva+json;data-type=edn`.

```JSON
{
    "meta": {
        "ver": 1,
        "data-type": "edn"
    },
    "data": "{{edn string}}"
}
```

## General JSON representation

This section describes the JSON formatted EDN representations. There is one unified structure that can always be used to
indicate the EDN representation for all types.

```JSON
{
    "tag": "{{tag}}",
    "type": "{{type}}",
    "value": {{value}},
}
```

The `tag` field may control the type of the value as in `#inst` and `#uuid`. Note that the `tag` field _may_ include the
hash `#` symbol, but it's not required. Furthermore the tag value may be set to `null` in which case the tag will not
be applied. The `type` field may be optional for some basic types, but it may be helpful in debugging scenarios to keep
it in. Also the `value` field is typically required (except the `nil`/`null` value). Though the long form is always
accepted, there may be times when the sort or implied values can be substituted. Finally, if the `tag` is set to the 
underscore `_`  than the value is treated as a discard in the EDN spec, and is not processed.

All values will be returned from the service as tightly as possible by default. To control the verbosity of the value,
add `verbosity=short` or `verbosity=long` to the `application/vnd.eva+json` mime type like so:
`application/vnd.eva+json;verbosity=long`. This flag is only available on the `application/vnd.eva+json;data-type=struct`
mime type. For completeness, adding the `verbosity` parameter can be made with the `data-type` parameter as such:
`application/vnd.eva+json;data-type=struct,verbosity=long`

| Verbosity Value | Description
|-----------------|-------------
| `implied`       | Will return the implied value if it can, otherwise the short value if it is available, otherwise the long value.
| `short`         | Will return the short value if it can, otherwise the long value.
| `long`          | Will always return the long value.

### Type Definitions

| EDN Type  | JSON type Value | type attribute     | value attribute  |
|-----------|-----------------|--------------------|------------------|
| Nil       | `nl`            | optional           | optional         |
| Boolean   | `bl`            | optional (implied) | json Boolean     |
| String    | `st`            | optional (implied) | json String      |
| Character | `c`             | required           | json String      |
| Symbol    | `sy`            | required           | json String      |
| Keyword   | `kw`            | required           | json String      |
| Integer   | `in`            | required           | json Number      |
| Float     | `fl`            | optional (implied) | json Number      |
| List      | `l`             | required           | json list        |
| Vector    | `v`             | optional (implied) | json list        |
| Map       | `m`             | required           | json map or list |
| Set       | `s`             | required           | json list        |
| EDN       | `edn`           | required           | edn string       |
| Comments  | `comment`       | requeired          | json String      |

#### Nil (`nl`) Type

This is the implementation for the `nil` EDN value.

Example EDN: `nil`

##### Implied Form

```JSON
null
```

##### Short Forms

Nils are a special case as it has two short forms, both can be sent into the client service and understood as `nil` values.
In the case where the client service needs to return the short form of a `nil` value, it will return the first such form
with the `value` field returned and not the `type` field.

```JSON
{
    "value": null
}
```

_or_

```JSON
{
    "type": "nl"
}
```

##### Long Form

```JSON
{
    "type": "nl",
    "value": null
}
```

#### Boolean (`bl`) Type

This is the implementation for the Boolean `true` or `false` EDN values.

Example EDN: `true`

##### Implied Form

```JSON
true
```

##### Short Form

```JSON
{
    "value": true
}
```

##### Long Form

```JSON
{
    "type": "bl",
    "value": true
}
```

#### String (`st`) Type

This is the implementation for the string EDN value.

Example EDN: `"my-value"`

##### Implied Form

```JSON
"my-value"
```

##### Short Form

```JSON
{
    "value": "my-value"
}
```

##### Long Form

```JSON
{
    "type": "st",
    "value": "my-value"
}
```

#### Character (`c`) Type

This is the implementation for the character EDN value.

| EDN Value  | JSON encoding |
|------------|---------------|
| `\return`  | `\r`          |
| `\newline` | `\n`          |
| `\space`   | ` `           |
| `\tab`     | `\t`          |

Also, like EDN, unicode characters can be returned using their codes: `\uNNNN`. Though JSON does support special
characters, it is expected to use just the escaped version without exploiting the extended unicode character set. This
is mostly due to the possible incompatibility to JSON encoders, though this assumption could be out of date.

Example EDN: `\return`

```JSON
{
    "type": "c",
    "value": "\r"
}
```

#### Symbol (`sy`) Type

This is the implementation for the symbol EDN value. There is only a long form representation for symbols.

Example EDN: `foo`

```JSON
{
    "type": "sy",
    "value": "foo"
}
```

#### Keyword (`kw`) Type

This is the implementation for the keyword EDN value. Note that the prefix `:` may be included but is not
required. There is only a long form representation for keywords.

Example EDN: `:foo/bar`

```JSON
{
    "type": "kw",
    "value": "foo/bar"
}
```

#### Integer (`in`) Type

This is the implementation for the integer EDN value. There is only a long form representation for integers. NOTE: some
users may wonder why integers have no short or implied versions. This is due to the JSON spec itself not differentiating
between integers and floats. Though some languages may understand floats or integers by the period separation, instead the
JSON spec indicates these are both numbers, thus we are ensuring the correct type is always transported.

Example EDN: `123`

```JSON
{
    "type": "in",
    "value": 123
}
```

#### Float (`fl`) Type

This is the implementation for the float EDN value. Note that the value EDN `123.0` may be transported without the
trailing `.0`. In this case, though it may look like an integer, the implied and short forms will be considered floats
anyway. This is due to the JSON spec not specifying a difference between floats and integers.

Example EDN: `123.45`
   
##### Implied Form

```JSON
123.45
```

##### Short Form

```JSON
{
    "value": 123.45
}
```

##### Long Form
   
```JSON
{
    "type": "fl",
    "value": 123.45
}
```

#### List (`l`) Type

This is the implementation for the list EDN value. There is only a long form representation for lists.

Example EDN: `(1, "string", :to/rule, "them all" )`

```JSON
{
    "type": "l",
    "value": [ 1, "string", { "type": "kw", "value": "to/rule"}, "them all" ]
}
```

#### Vector (`v`) Type

This is the implementation for the vector EDN value.

Example EDN: `[1, "string", :to/rule, "them all" ]`

##### Implied Form

```JSON
[ 1, "string", { "type": "kw", "value": "to/rule"}, "them all" ]
```

##### Short Form

```JSON
{
    "value": [ 1, "string", { "type": "kw", "value": "to/rule"}, "them all" ]
}
```

##### Long Form

```JSON
{
    "type": "v",
    "value": [ 1, "string", { "type": "kw", "value": "to/rule"}, "them all" ]
}
```

#### Map (`m`) Type

This is the implementation for the map EDN value. Maps are special and have two formats that don't quite look the same.
The long version has an embedded structure where the short form has an array of arrays. These different representations
may need special processing rules on your implementation.

Example EDN: `{ "one" 123 :other nil }`

##### Short Form

Note that the short version has an array or arrays where the inner array can only have two elements representing the `key` and
`value` in this order.

```JSON
{
    "type": "m",
    "value": [
        [ "one", 123 ],
        [{ "type": "kw", "value": "other" }, null ]
    ]
}
```

##### Long Form

```JSON
{
    "type": "m",
    "value": [
        {
            "key": "one",
            "value": 123
        },
        {
            "key": {
                "type": "kw",
                "value": "other"
            },
            "value": null
        }
    ]
}
```

#### Set (`s`) Type

This is the implementation for the set EDN value. There is only a long form representation for sets.

Example EDN: `#{1, "string", :to/rule, "them all" }`

```JSON
{
    "type": "s",
    "value": [ 1, "string", { "type": "kw", "value": "to/rule"}, "them all" ]
}
```

#### EDN (`edn`) Type

For some cases having a JSON format may be more difficult to generate and passing the EDN directly may be desirable.
There is no way to return this type through the `verbosity` field.

Example EDN: `[:find ?b :in $ ?t :where [?b :book/title ?t]]`

```JSON
{
    "type": "edn",
    "value": "[:find ?b :in $ ?t :where [?b :book/title ?t]]"
}
```

#### Comment (`comment`) Type

Users may want to add comments to their requests. This is generally just used for humans to understand what the system
may be doing. These comments are never returned by the service.

Example EDN: `;this is a comment and is generally ignored.`

```JSON
{
    "type": "comment",
    "value": "this is a comment and is generally ignored."
}
```

#### Special Types

The following types and their examples use tags. As was stated above, the `tag` JSON field optionally specified the `#`
prefix. If the value is returned from the service, the `#` prefix will be omitted.

##### Instance

For instance in time this is the layout.

Example EDN: `#inst "1985-04-12T23:20:50.52Z"`

```JSON
{
    "tag": "inst",
    "type": "st",
    "value": "1985-04-12T23:20:50.52Z"
}
```

##### UUID

For instance in UUIDs this is the layout.

Example EDN: `#uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"`

```JSON
{
    "tag": "uuid",
    "type": "st",
    "value": "81d4fae-7dec-11d0-a765-00a0c91e6bf6"
}
```

##### Temporary IDs

For instance in temporary IDs this is the layout.

Example EDN: `#db/id[:db.part/user -1]`

```JSON
{
    "tag": "db/id",
    "type": "v",
    "value": [
        {
            "type": "kw",
            "value": "db.part/user"
        },
        {
            "type": "in",
            "value": -1
        }
    ]
}
```

##### Datom

The following is an example of the datom that can be returned.

Example EDN: `#datom[4398046511107 15 "Value" 4398046511107 true]`

###### Implied Form

```JSON
{
    "tag": "datom",
    "type": "v",
    "value": [
        {
            "type": "in",
            "value": 4398046511107
        },
        {
            "type": "in",
            "value": 15
        },
        "Value",
        {
            "type": "in",
            "value": 4398046511107
        },
        true
    ]
}
```

###### Short Form

```JSON
{
    "tag": "datom",
    "type": "v",
    "value": [
        {
            "type": "in",
            "value": 4398046511107
        },
        {
            "type": "in",
            "value": 15
        },
        {
            "value": "Value"
        },
        {
            "type": "in",
            "value": 4398046511107
        },
        {
            "value": true
        }
    ]
}
```

###### Long Form

```JSON
{
    "tag": "datom",
    "type": "v",
    "value": [
        {
            "type": "in",
            "value": 4398046511107
        },
        {
            "type": "in",
            "value": 15
        },
        {
            "type": "st",
            "value": "Value"
        },
        {
            "type": "in",
            "value": 4398046511107
        },
        {
            "type": "bl",
            "value": true
        }
    ]
}
```

##### Connection Reference

The following is the connection reference implementation. Note that this is a special type and has two representations
with either the `vector` or `map` forms being value. We will use the `vector` implementation as the short form and the
`map` representation as the long form for the sake of the `verbosity` parameter in the mime type.

###### Vector/Short Form

Example EDN: `#eva.client.service/connection-ref [ "test-server" ]`

```JSON
{
    "tag": "eva.client.service/connection-ref",
    "type": "v",
    "value": [
        "test-server"
    ]
}
```

###### Map/Long Form

Example EDN: `#eva.client.service/connection-ref { :label "test-server" }`

```JSON
{
    "tag": "eva.client.service/connection-ref",
    "type": "m",
    "value": [
        [
            {
                "type": "kw",
                "value": "label"
            },
            {
                "type": "st",
                "value": "test-server"
            }
        ]
    ]
}
```

##### Shapshot Reference

The following is the shapshot reference implementation. Note that this is a special type and has two representations
with either the `vector` or `map` forms being value. We will use the `vector` implementation as the short form and the
`map` representation as the long form for the sake of the `verbosity` parameter in the mime type. As with the EDN spec,
the `as-of` field may be omitted from a request to get the most recent shapshot, but the service will always return
this field.

###### Vector/Short Form

Example EDN: `#eva.client.service/snapshot-ref [ "test-server" 5 ]`

```JSON
{
    "tag": "eva.client.service/snapshot-ref",
    "type": "v",
    "value": [
        "test-server",
        {
            "type": "in",
            "value": 5
        }
    ]
}
```

###### Map/Long Form

Example EDN: `#eva.client.service/snapshot-ref { :label "test-server" :as-of 5 }`

```JSON
{
    "tag": "eva.client.service/snapshot-ref",
    "type": "m",
    "value": [
        [
            {
                "type": "kw",
                "value": "label"
            },
            {
                "type": "st",
                "value": "test-server"
            }
        ],
        [
            {
                "type": "kw",
                "value": "as-of"
            },
            {
                "type": "in",
                "value": 5
            }
        ]
    ]
}
```

##### Inline Functions

The following is the inline function call implementation. Note that this is a special type and has two representations
with either the `map` or `vector` forms being value. We will use the `vector` implementation as the short form and the
`map` representation as the long form. 

Example EDN: `#eva.client.service/inline [ first [ 1 2 3 ] ]`

###### Short Form

```JSON
{
    "tag": "eva.client.service/inline",
    "type": "v",
    "value": [
        {
            "type": "sy",
            "value": "first"
        },
        [
            {
                "type": "in",
                "value": 1
            },
            {
                "type": "in",
                "value": 2
            },
            {
                "type": "in",
                "value": 3
            }
        ]
    ]
}
```

###### Long Form

```JSON
{
    "tag": "eva.client.service/inline",
    "type": "m",
    "value": [
        {
            "key": {
                "type": "kw",
                "value": "fn"
            },
            "value": {
                "type": "sy",
                "value": "first"
            }
        },
        {
            "key": {
                "type": "kw",
                "value": "params"
            },
            "value": {
                "type": "v",
                "value": [
                    {
                        "type": "in",
                        "value": 1
                    },
                    {
                        "type": "in",
                        "value": 2
                    },
                    {
                        "type": "in",
                        "value": 3
                    }
                ]
            }
        }
    ]
}
```

## Examples

Here are some examples to get you started.

### Schema Transact Element

Creating a new attribute using the transact endpoint.

#### EDN

`Content-Type` header set to `x-www-form-urlencoded`.

_`reference` field_
```clojure
#eva.client.service/connection-ref [ "test-service" ]
```

_`transaction` field_
```clojure
[
    {
        :db/id #db/id [:db.part/user]
        :db/ident :book/title
        :db/doc "Title of a book"
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db.install/_attribute :db.part/db
    }
]
```

### JSON

`Content-Type` header set to `application/vnd.eva+json`.

```JSON
{
    "meta": {
        "ver": 1,
        "data-type": "fields"
    },
    "data": {
        "reference": {
            "tag": "eva.client.service/connection-ref",
            "type": "v",
            "value": [
                "test-server"
            ]
        },
        "transaction": [
            {
                "type": "m",
                "value": [
                    [
                        {
                            "type": "kw",
                            "value": "db/id"
                        },
                        {
                            "type": "v",
                            "value": [
                                {
                                    "type": "kw",
                                    "value": "db.part/user"
                                }
                            ]
                        }
                    ],
                    [
                        {
                            "type": "kw",
                            "value": "db/ident"
                        },
                        {
                            "type": "kw",
                            "value": "book/title"
                        }
                    ],
                    [
                        {
                            "type": "kw",
                            "value": "db/doc"
                        },
                        "Title of a book"
                    ],
                    [
                        {
                            "type": "kw",
                            "value": "db/valueType"
                        },
                        {
                            "type": "kw",
                            "value": "db.type/string"
                        }
                    ],
                    [
                        {
                            "type": "kw",
                            "value": "db/cardinality"
                        },
                        {
                            "type": "kw",
                            "value": "db.cardinality/one"
                        }
                    ],
                    [
                        {
                            "type": "kw",
                            "value": "db.install/_attribute"
                        },
                        {
                            "type": "kw",
                            "value": "db.part/db"
                        }
                    ]
                ]
            }
        ]
    }
}
```

#### Response

##### EDN

If the `Accept` header is set to `application/vnd.eva+edn`, then the result would look like this.

```clojure
{
    :tempids {-9223363240760753595 8796093023232},
    :db-before #eva.client.service/snapshot-ref [ books 1 ],
    :eva.client.service/tempids {#db/id[:db.part/user -1000005] 8796093023232},
    :tx-data (#datom[4398046511106 15 #inst "2018-06-12T02:51:10.982-00:00" 4398046511106 true]),
    :db-after #eva.client.service/snapshot-ref [ books 2 ]
}
````

##### JSON

If the `Accept` header is set to `application/vnd.eva+json`, then the result would look like this.

```JSON
{
    "meta": {
        "ver": 1,
        "data-type": "struct"
    },
    "data": {
        "type": "m",
        "value": [
            [
                {
                    "type": "kw",
                    "value": "tempids"
                },
                {
                    "type": "m",
                    "value": [
                        {
                            "type": "in",
                            "value": -9223363240760753595
                        },
                        {
                            "type": "in",
                            "value": 8796093023232
                        }
                    ]
                }
            ],
            [
                {
                    "type": "kw",
                    "value": "db-before"
                },
                {
                    "tag": "eva.client.service/snapshot-ref",
                    "type": "v",
                    "value": [
                        {
                            "type": "sy",
                            "value": "book"
                        },
                        {
                            "type": "in",
                            "value": 1
                        }
                    ]
                }
            ],
            [
                {
                    "type": "kw",
                    "value": "eva.client.service/tempids"
                },
                {
                    "type": "m",
                    "value": [
                        [
                            {
                                "tag": "db/id",
                                "type": "v",
                                "value": [
                                    {
                                        "type": "kw",
                                        "value": "db.part/user"
                                    },
                                    {
                                        "type": "in",
                                        "value": -1000005
                                    }
                                ]
                            },
                            8796093023232
                        ]
                    ]
                }
            ],
            [
                {
                    "type": "kw",
                    "value": "tx-data"
                },
                {
                    "tag": "datom",
                    "type": "v",
                    "value": [
                        {
                            "type": "in",
                            "value": 4398046511106
                        },
                        {
                            "type": "in",
                            "value": 15
                        },
                        {
                            "tag": "inst",
                            "type": "st",
                            "value": "2018-06-12T02:51:10.982-00:00"
                        },
                        {
                            "type": "in",
                            "value": 4398046511106
                        },
                        {
                            "type": "bl",
                            "value": true
                        }
                    ]
                }
            ],
            [
                {
                    "type": "kw",
                    "value": "db-after"
                },
                {
                    "tag": "eva.client.service/snapshot-ref",
                    "type": "v",
                    "value": [
                        {
                            "type": "sy",
                            "value": "book"
                        },
                        {
                            "type": "in",
                            "value": 2
                        }
                    ]
                }
            ]
        ]
    }
}
```

### Query Example

In this example we will pass the query and raw EDN. This may be helpful for queries that are easier on the 
developer then writing out the entire structure. Note that the parameters are numbered like the traditional
REST service.

#### Request

##### EDN

`Content-Type` header set to `x-www-form-urlencoded`.

_`query` field_

```clojure
[:find ?b :in $ ?t :where [?b :book/title ?t]]
```

_`p[0]` field_

```clojure
#eva.client.service/snapshot-ref [ "my-snapshot" ]
```

_`p[1]` field_

```clojure
"First Book"
```

##### JSON

`Content-Type` header set to `application/vnd.eva+json`.

```JSON
{
    "meta": {
        "ver": 1,
        "data-type": "fields"
    },
    "data": {
        "query": {
            "type": "edn",
            "value": "[:find ?b :in $ ?t :where [?b :book/title ?t]]"
        },
        "p[0]": {
            "tag": "eva.client.service/snapshot-ref",
            "type": "v",
            "value": [
                "my-snapshot"
            ]
        },
        "p[1]": "First Book"
    }
}
```

#### Response

##### EDN

If the `Accept` header is set to `application/vnd.eva+edn`, then the result would look like this.

```clojure
[[8796093023242]]
````

##### JSON

If the `Accept` header is set to `application/vnd.eva+json`, then the result would look like this.

```JSON
{
    "meta": {
        "ver": 1,
        "data-type": "struct"
    },
    "data": [
        [
            {
                "type": "in",
                "value": 8796093023242
            }
        ]
    ]
}
```
