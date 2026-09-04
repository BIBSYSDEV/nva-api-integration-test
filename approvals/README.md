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
They are seeded by `test_data/create_approval_clients.py` in
[NVA-end-to-end-testing](https://github.com/BIBSYSDEV/NVA-end-to-end-testing), which runs as part
of `create_test_data.py` whenever the e2e test data is regenerated.

That script creates one external client per customer and stores it as a secret these tests read
(`ApiTestApprovalClientUib` and `ApiTestApprovalClientUis`), writes the `IdentifierPolicy` item
deciding which identifier names each customer may use, and points `/test/ApprovalsTable` at the
approvals table.

There is no API for writing an identifier policy, so it is written straight to DynamoDB.

Rerunning is safe: an existing client is left alone, while the policies and the parameter are
rewritten so drift is corrected.

The customers are UiB and UiS, allowed to write `apitest-uib` and `apitest-uis` respectively.

## Running

```
./gradlew :approvals:test -PawsProfile=nva-e2e
```
