# Cristin API tests

## Person lookup by national identity number

`LookupPersonByNinApiTest` tests `POST /cristin/person/identityNumber` in NVA's Cristin proxy as a
third party. API Gateway accepts the request only with the `third-party/person-lookup-by-nin`
scope, and the handler has no scope check of its own, so these tests are where the scope rejection
is verified.

The tests check that a lookup without a token, and a lookup with a valid token that lacks the
scope, are both rejected. The token comes from `ApiTestApprovalClientUib`, the external client
the approval tests use, which is seeded by
[NVA-end-to-end-testing](https://github.com/BIBSYSDEV/NVA-end-to-end-testing) with the
`third-party/approval-upsert` scope only.

The request body carries a random synthetic national identity number. The number is redacted from
the Allure report, and request logging on failure is turned off for these requests, so it never
reaches test output.

```
./gradlew :cristin-api:test --tests '*LookupPersonByNinApiTest' -PawsProfile=nva-e2e
```
