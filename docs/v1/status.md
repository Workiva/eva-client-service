# Status

<!-- toc -->

- [Description](#description)
- [GET /eva/v.1/status/{tenant}/{category}/{label}](#get-evav1statustenantcategorylabel)
  * [Example](#example)

<!-- tocstop -->

## Description

A simple endpoint that returns status information on a given connection.  The results are returned as a map, possible returned values are as follows:

| Key | Type | Description
| --- | ---- | ---
| `latestT` | long | The latest transaction number for a given Connection.

## GET /eva/v.1/status/{tenant}/{category}/{label}

### Example

#### cURL Command

```bash
curl -X GET \
  http://localhost:8080/eva/v.1/status/test-tenant/test-category/test-label \
  -H 'Accept: application/vnd.eva+edn' \
  -H 'Cache-Control: no-cache' \
  -H 'Postman-Token: 7f612a24-01c9-4ff5-9eb3-fb3a89cd24ab'
```

#### Example Result

```clj
{
  :latestT 0
}
```
