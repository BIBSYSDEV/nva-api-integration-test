# NVI integration test findings (2026-06-09)

Issues and follow-ups discovered while adding the `scientific-index` API test module.
For review and follow-up; none of these block the new tests, which are green against e2e.

## 1. `nva-nvi/docs/openapi.yaml` is out of date

Tracked in [NP-51332](https://sikt.atlassian.net/browse/NP-51332).
The documented candidate response does not match the actual `CandidateDto` returned by the service.

| OpenAPI says                                       | Service returns                                                |
| -------------------------------------------------- | -------------------------------------------------------------- |
| `approvalStatus` array                             | `approvals` array (`ApprovalDto`)                              |
| no top-level `identifier`                          | `identifier` (UUID)                                            |
| `periodStatus` object                              | `period` object (`PeriodStatusDto`, type `NviReportingPeriod`) |
| approval status enum `Approved, Pending, Rejected` | also returns `New` (unassigned pending) and `None`             |
| `NviPeriods` as bare array                         | `{ "periods": [...] }` (`NviPeriodsResponse`)                  |

Source: `nva-nvi/nvi-commons/src/main/java/no/sikt/nva/nvi/common/service/dto/CandidateDto.java`, `ApprovalDto.java`,
`ApprovalStatusDto.java`, and `nvi-rest/.../model/NviPeriodsResponse.java`.
Follow-up: update the OpenAPI spec (or generate it from the DTOs).

## 2. `PublicationFactory` test data is not NVI-eligible

Publications built by the shared `PublicationFactory` (publication-api testFixtures) are silently evaluated as
`NonNviCandidate`.
Two data problems, confirmed by comparing against a publication that did become a candidate in e2e:

1. **Channel IRI lacks the year segment.**
   The fixture `AcademicArticleReference.json` uses `.../serial-publication/7ECF363E-.../` without `/{year}`.
   The NVI evaluator requires the channel IRI to end with the publication year and logs
   `Publication channel year does not match IRI` / `Publication channel IRI is invalid`.
2. **Contributor `identity.id` is a Cristin username, not a person URI.** (FIXED in main)
   The factory used to set `identity.id = "1862458@184.0.0.0"`.
   The evaluator parses the expanded publication as JSON-LD with SPARQL, and a non-IRI id makes the contributor
   disappear entirely (`Publication does not have at least one verified or unverified contributor`,
   `Publication top-level organization is missing`).
   As of commit b098e52, `PublicationFactory` now emits `https://.../cristin/person/{id}`, so this half is resolved
   upstream and `NviCandidateFactory` no longer patches contributor ids.

Workaround: `NviCandidateFactory` in the new `scientific-index` module still patches the channel IRI (problem 1) before
publishing.
Follow-up: consider upstreaming the channel-year fix into `PublicationFactory` / `AcademicArticleReference.json` so all
modules produce realistic publications.

## 3. Transient 409 when mutating a candidate right after creation

`PUT /scientific-index/candidate/{id}/status` intermittently returned 409 Conflict within the first seconds after the
candidate appeared.
Observed once for an otherwise deterministic 400 case (reject without reason), which returned 409 on one run and 400 on
the next.
Cause: the publish flow emits several expanded-resource events in quick succession, and trailing re-evaluations of the
same publication race the status update, hitting a DynamoDB transaction conflict (`TransactionException` mapped to 409
in `ExceptionMapper`).
Workaround: the test helper retries 409 responses for up to 30 seconds.
Follow-up: decide if this is acceptable API behavior (clients must retry 409s) or if the evaluator/update path should be
made idempotent or serialized.

## 4. No API surface explains why a publication is not a candidate

When evaluation rejects a publication, the reason is only visible as WARN/INFO lines in the
`EvaluateNVICandidateHandler` Lambda logs.
`GET /candidate/publication/{id}` just returns 404, and `report-status` returns `NOT_CANDIDATE` without a reason.
This made the test failures above expensive to diagnose and will hit curators and integrators too.
Follow-up: consider exposing evaluation problems (the response already has a `problems` array) or at least structured
log events.

Debugging pointers for next time:

- CloudWatch log group: `/aws/lambda/master-pipelines-NvaNvi-1-EvaluateNVICandidateHand-*` (e2e), search for
  `Model validation failed` and `Evaluated ... as NonNviCandidate`.
- DynamoDB table `nva-nvi-master-...-nva-nvi`, GSI `SearchByPublicationId`, hash key = raw publication URI (no prefix).

## 5. Messages in the e2e EvaluationDLQ

The NVI `EvaluationDLQ` in e2e held 10 messages at the time of testing (example body referenced persisted resource
`019e73c063ee-...`, older than our test run).
These are evaluation events that failed after retries and were never processed.
Follow-up: inspect and requeue or drain (there is an `NviRequeueDlqHandler` Lambda), and check whether an alarm exists
on DLQ depth.

## 6. `GET /period` (list) requires MANAGE_NVI, single period is public

Tracked in [NP-51333](https://sikt.atlassian.net/browse/NP-51333), pending product owner clarification.
Listing periods requires the `MANAGE_NVI` access right (admin), while `GET /period/{year}` is unauthenticated.
This seems inconsistent: the list contains nothing more sensitive than the individual resources.
It also means no existing api-test user can list periods (the NVI curator only has `MANAGE_NVI_CANDIDATES`).
Follow-ups:

- Provision an api-test user with `MANAGE_NVI` (e.g. `api-test-user-nvi-admin-uib@test.sikt.no`) in the e2e identity
  service.
  This unblocks the `@Disabled` test in `ListPeriodsApiTest` and future period-write tests.
- Consider relaxing auth on the list endpoint in nva-nvi.

## 7. Periods cannot be deleted

There is no DELETE endpoint for NVI periods, so any period created by a test pollutes the shared environment
permanently.
This is why the new module only has read-only period tests.
Follow-up: decide on a strategy for testing create/update (a sacrificial far-future period like 9999 was discussed, or a
delete endpoint guarded by admin rights).

## Deferred test scope (not bugs)

- Assignee endpoint (`PUT /candidate/{id}/assignee`) and notes (create/delete; note identifier is missing from the
  OpenAPI response schema and must be inspected live).
- Search tests (`GET /candidate?...`), which depend on OpenSearch indexing latency.
- Period create/update tests, pending items 6 and 7.
- Test publications and candidates are left in e2e after runs (UUID-tagged titles), matching existing repo practice.
