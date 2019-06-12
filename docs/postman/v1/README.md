# Using Postman

## Setup Postman

Download and Install from [https://www.getpostman.com/](https://www.getpostman.com/)

Postman does offer professional and enterprise tier application versions, as well as sign-on / syncing capabilities.  But neither of these are required to use the provided examples and build up a library of your own requests, **signing in and making an account is not required**.

Once Postman is setup and open we can import the required example files.  Click the `Import` Button on the top left and import the following two files.

- colls/EVA_101.postman_collection.json
  - Once imported you should see a new collection on the left sidebar titled `Eva-Client-Service - Eva 101`
- envs/EVA_LOCAL.postman_environment.json
  - Once imported you should have a `Local Eva` option available to you on the top-right dropdown menu.  This holds all of the placeholder variables used in the requests.

The following key-values pairs are defined in the environment:
<!-- these may change once cross-database references are ironed out -->

| Key               | Example value    | Description
| ----------------- | ---------------- | ------------
| `{{server}}`      | `localhost`      | The hostname that the eva-client-service is running on
| `{{port}}`        | `8080`           | The port that eva-client-service is listening on
| `{{tenant}}`      | `RandDWorkiva`   | The tenant for a configuration is a partitioning of ownership for configurations.
| `{{category}}`    | `example`        | The category for a configuration indicates a cross-configuration coupling. 
| `{{label}}`       | `books`          | The label on a configuration is a name that, along with the tenant and category uniquely identifies a configuration for a specific database.

## Running Examples

Ensure that you have the `Eva Local` environment select on the top-right dropdown, and that you have the client service running locally on port 8080, either through docker or running `make run-local`.

Select a request on the left and then click `Send`.  These sample requests can be duplicated and experimented on to make your own collection of requests.

## Writing Tests

When running all of the `Eva 101` tests, you may have seen a tab in the response called `Test Results (n/y)`.  This is because each request has some simple tests defined in the `Tests` tab.  These tests are defined in Javascript, are exported within the collection and can be ran and verified en masse using [newman](https://www.getpostman.com/docs/v6/postman/collection_runs/command_line_integration_with_newman)

**Example Tests**
```js
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Body matches string", function () {
    pm.expect(pm.response.text()).to.include(":tempids");
    pm.expect(pm.response.text()).to.include(":db-before");
    pm.expect(pm.response.text()).to.include(":tx-data");
    pm.expect(pm.response.text()).to.include(":db-after");
});
```

> Simply checks that the request turns with a successful `200` and that the transaction response has the map keys we would expect.

### Batch Running & Verifying Tests with `newman`

Running all of your requests and verifying the responses could be useful when experimenting locally, developing on the client service, or for CI purposes.

To run all the tests of the entire Eva 101 collection, perform the following:

```bash
npm install -g newman
newman run colls/EVA_101.postman_collection.json -e envs/EVA_Skynet.postman_environment.json
```
