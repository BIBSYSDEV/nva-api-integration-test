# Approval API tests

The `approvals` module tests the Approval API in nva-handle-service, which associates persistent
handle identifiers with named identifiers from external systems.

The read endpoints (`/approval/{approvalId}`, `/approval?handle=`, `/approval/context`,
`/approval/ontology`) are open, and testing them needs nothing beyond the usual AWS credentials.

Writing an approval is different. `POST /approval` and `PUT /approval/{approvalId}` require a
client credentials token with the `third-party/approval-upsert` scope, and the client's customer
decides which identifier names it is allowed to write. That authorization is per customer, so the
tests need two external clients on two different customers: one writes an identifier name, and the
other is rejected when it tries to write a name belonging to the first.

## Where the clients come from

External clients cannot be created from a test, since there is no way to delete one afterwards.
They are seeded by `test_data/create_external_clients.py` and
`test_data/create_approval_policies.py` in
[NVA-end-to-end-testing](https://github.com/BIBSYSDEV/NVA-end-to-end-testing), which run as part
of `create_test_data.py` whenever the e2e test data is regenerated.

The first creates one external client per customer and stores it as a secret these tests read
(`ApiTestApprovalClientUib` and `ApiTestApprovalClientUis`). The second writes the
`IdentifierPolicy` item deciding which identifier names each customer may use, and points
`/test/ApprovalsTable` at the approvals table.

There is no API for writing an identifier policy, so it is written straight to DynamoDB.

Rerunning is safe: an existing client is left alone, while the policies and the parameter are
rewritten so drift is corrected.

The customers are UiB and UiS, allowed to write `apitest-uib` and `apitest-uis` respectively.

A policy also lists the sources its customer may point approvals at, and a write whose `source`
matches none of them is forbidden. The source must also resolve, since the API fetches it to
snapshot it and to render the approval page. Both policies therefore register the API root of the
environment (`https://<api domain>/`) without authentication, and the tests use known Cristin
organizations such as UiB and UiS as sources. The UiB policy registers the clinical trial fixture
bucket as a second source, for the tests that need an approval the search index accepts.

## Where the clinical trial comes from

Indexing accepts clinical trial sources only, so an approval created from anything else is never
searchable and a query can never find it. CTIS has no stable test environment, so the tests serve
a captured trial document instead of fetching one from CTIS.

It is served from `approval-source-fixtures-<account-id>`, created by
`test_data/create_fixture_bucket.py` in
[NVA-end-to-end-testing](https://github.com/BIBSYSDEV/NVA-end-to-end-testing), which runs as part
of `create_test_data.py` alongside the scripts above. The API fetches a source without
credentials, so the bucket lets anyone read its objects, and it holds nothing but this fixture.

`ClinicalTrialSource` uploads `clinical-trial.json` there before the approval is created, so the
test runner role needs `s3:PutObject` on the bucket. The document is stored as it came from CTIS
rather than built from a record, since the API parses it against its own bundled CTIS context and
a shape invented here would prove nothing about the real one.

The bucket is not under the API root, so `create_approval_policies.py` registers it as a second
source config on the UiB policy. Its base uri is written as `{fixtureBucketUrl}` in
`identifier_policies.json` and resolved at seed time, the same way `{apiUrl}` is, since the bucket
name carries the account id of the environment being seeded.

## Running

```
./gradlew :approvals:test -PawsProfile=nva-e2e
```
