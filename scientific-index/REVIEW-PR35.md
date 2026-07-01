# PR #35 Review — feat: Add NVI tests

Branch: `larsv/2026/06/26-nvi-tests` → `main`

Adds a new `scientific-index` Gradle module with REST-Assured integration tests for the NVI candidate, approval, and period APIs, plus an `NviCandidateFactory` fixture that publishes an NVI-eligible academic article and waits for it to be evaluated into a candidate.
Also extends the shared `AcademicArticleReference.json` fixture with a full journal channel (id, ISSN, scientific value).

Status legend: `[ ]` open, `[x]` fixed, `[~]` won't fix / deferred.

## Findings (most severe first)

### [x] 1. Hardcoded `/2026` in shared `AcademicArticleReference.json` is a time-bomb

File: `publication-api/src/testFixtures/resources/metadata/AcademicArticleReference.json:17,26`
Severity: high

The channel `id` ended with `/2026` and `"year": "2026"` was hardcoded, while `PublicationFactory.createEntityDescription` sets `publicationDate.year = CURRENT_YEAR`.
`createReference` only appended `CURRENT_YEAR` to a `publisher.id` (line 245); a Journal context has no `publisher`, so the channel IRI stayed pinned to `/2026`.
NVI eligibility requires the channel IRI year to match the publication year.

Failure: when the suite runs in 2027, `CURRENT_YEAR=2027` no longer matches the `/2026` channel, the publication is classified `NonNviCandidate`, `awaitCandidate` polls the full 3 minutes and never finds a candidate, and every test in `FetchCandidateApiTest` and `UpdateApprovalStatusApiTest` fails.

Fixed (2026-06-26): replaced the in-code year-appending logic in `PublicationFactory.createReference` with a `{year}` placeholder that the fixtures carry explicitly. `loadJsonResource` now reads each fixture as a string and substitutes `{year}` with `CURRENT_YEAR` before parsing (new `readFixture` helper, `YEAR_PLACEHOLDER` constant). `createReference` is reduced to a plain load + `getMap`. The fixture authors decide which channel ids get a year by placing the token.

Placeholder added to: the journal channel `id` (AcademicArticleReference) and every `publisher.id` (AcademicMonograph, BookAnthology, ConferenceReport, DegreeMaster, DegreePhD, ReportResearch). Left year-less: the nested `series.id`s (UnconfirmedSeries) and the Anthology `id: ""`. Removed the static `"year": "2026"` field from AcademicArticleReference.

Behavior-preserving except for the intended fix: `publisher.id` still resolves to `.../publisher/{uuid}/{CURRENT_YEAR}` (byte-identical to the old in-code append), `series.id` stays year-less as before, and only the journal `id` changes (now year-versioned instead of frozen at `/2026`). Verified `:publication-api:compileTestFixturesJava` compiles and `:publication-api:spotlessJavaCheck` passes.

Note for non-current-year tests: the reporting year is now a single substitution point in `loadJsonResource`. To support a year other than `CURRENT_YEAR`, thread a year argument through `loadJsonResource`/`createReference` and substitute that instead (the publication date in `createEntityDescription` would need the same year to stay consistent).

### [ ] 2. `createNviEligiblePublication` re-implements `PublicationFactory.createPublishedPublication`

File: `scientific-index/src/testFixtures/java/no/sikt/nva/apitest/scientificindex/NviCandidateFactory.java:52-66`
Severity: medium (reuse)

The hand-rolled body (create draft → remove `@context` → `createEntityDescription` → `updatePublication` → `publish` → return identifier) is exactly what `createPublishedPublication(user, title, category, contributors, curator)` already does (via `createPublishedPublicationWithReferenceUsingTokens` with an empty reference map), and that method already takes a separate author and curator.

Failure: when the publish contract changes upstream, the factory method gets fixed while this copy silently rots and the NVI tests test a stale flow.

### [x] Flakiness: async read-after-write race (intermittent pass/fail)

Files: `NviCandidateFactory.awaitCandidate`, `IntegrationTestBase.configureRestAssured`, `UpdateApprovalStatusApiTest.updateApprovalStatus`
Severity: high (flaky suite)

Root cause: `awaitCandidate` gated only on the candidate endpoint returning 200 (existence), but the evaluator populates `approvals`/`totalPoints` asynchronously and trailing re-evaluation events keep upserting the candidate afterwards. Tests then asserted on that not-yet-settled state. Parallel execution (enabled in `build-logic/.../nva.api.integration.testing.java-conventions.gradle`, concurrent methods + classes) amplified it and blew the fixed timeouts.

Fixed (2026-06-29):
- `NviCandidateFactory.awaitCandidate` now polls until the candidate is fully evaluated (`approvals` non-empty and `totalPoints` a positive number), with `ignoreExceptions()` for transient partial responses, and reads the identifier from that same settled response (also removes the double-fetch from review finding #5). Uses `HttpURLConnection.HTTP_OK` instead of a literal.
- `UpdateApprovalStatusApiTest`: 409-retry window widened from 30s to `CONFLICT_RETRY_TIMEOUT_SECONDS` (120s); see finding #3 below.
- `IntegrationTestBase.configureRestAssured` made run-once and thread-safe (static `synchronized` + guard flag). It only ever set global `RestAssured` state to identical values, so re-running it per class raced the config/filter reassignment against in-flight requests from concurrently-executing classes.

Verified compile, Spotless, and PMD on `apitest-base` and `scientific-index`. Not run against live e2e.

Follow-up (2026-06-30): a real run surfaced the dominant cause via the new login diagnostics: `CognitoLogin` did a full two-request OAuth login on every authenticated request (no caching), so under parallel execution the Cognito pre-token-generation Lambda was throttled (`UserLambdaValidationException: Rate exceeded`, HTTP 400). Two fixes in `CognitoLogin`:
- Surfaced the failure: `login`/`getCode` use `.noFilters()` (which strips the global validation-failure logging) and RestAssured's `.statusCode()` omits the body, so replaced those with a `requireStatus` helper that throws `IllegalStateException` including status + response body (visible because Gradle prints exceptions but hides stdout). Never logs the request body (password) or successful token bodies.
- Per-user token cache (`ConcurrentHashMap` via `compute`, keyed on `userId`, expiring on the token's own `expires_in` minus a 60s safety margin). Collapses the auth storm to one login per user, re-authenticates automatically if a long run outlives the token, and the per-key lock prevents a same-user first-login stampede.

Follow-up (2026-06-30): the token-cache speedup then exposed a latent race in `BibTexTest.shouldReturnListOfPublicationsInBibTexFormat`. It created 6 publications but waited for only the last-created one to be indexed, then searched expecting all 6. OpenSearch indexing is async with no ordering guarantee, so the earlier docs were not necessarily indexed yet (the slow per-publication logins had previously masked this). Fixed by `awaitIndexedPublications(query, expectedCount)`, which polls the shared query until all expected entries are present before asserting. Single-publication tests still use `waitForIndexing` (one doc is indexed atomically, so non-empty implies complete).

Sweep (2026-06-30) of all search tests for the same "wait for a proxy, assert on aggregate" pattern:
- `BibTexTest` single-publication tests: safe (one atomic doc, unique-id wait).
- `BibTexVolumeTest.shouldReturnAllPublicationsInBibTexFormat`: already a correct count-based wait (`X-Total-Count == 100`). Bumped its `atMost` from 60s to 120s, indexing 100 docs under parallel load can exceed 60s and the faster cached auth makes it start polling sooner; widening is pure upside since await exits as soon as the count is reached.
- `BibTexVolumeTest` link-header / no-hits tests: robust by construction (assert no specific indexed count). No change.
No other instances of the broken pattern found.

Follow-up (2026-07-01), two more:
- `BibTexTest.shouldReturnListOfPublicationsInBibTexFormat` blank-line assertion was a wrong expectation (exposed once the entry-count assertion reliably passed): it expected `(n-1)*2` blank lines, but "two newlines between publications" is a single blank line per gap in `String.lines()`, so the correct count is `n-1` (confirmed: 4 entries→3 blanks, 6→5). Fixed the expected value.
- NVI `awaitCandidate` intermittently timed out at 3m because `UpdateApprovalStatusApiTest`'s 5 methods each create a candidate and ran concurrently, bursting ~6 async evaluations at once and backing up the pipeline. Added `@Execution(SAME_THREAD)` to that class so at most one candidate evaluates at a time (peak concurrent evaluations drop to ~2, counting `FetchCandidateApiTest`'s shared one). Also bumped the candidate wait 3m→5m and the scientific-index JUnit default timeout 5m→10m to fit worst-case `create (5m) + approval-conflict retry (2m)`.

Follow-up (2026-07-01): `BibTexTest.shouldReturnPublicationsInBibTexFormat` failed at `waitForIndexing`'s trailing `assertThat(getResponseBody(query)).isNotEmpty()`. The Awaitility loop had already seen a non-empty body, but that redundant re-fetch returned empty: OpenSearch search results flicker near the visibility boundary (consecutive requests can hit replicas at different refresh states), so any re-fetch after the wait is racy (same double-fetch TOCTOU removed earlier from the candidate factory). Fixed by making `waitForIndexing` return the body captured during polling and having all single-publication tests assert on that snapshot instead of issuing another request. Added `ignoreExceptions()` to both search waits so a transient error during indexing retries instead of failing. The disabled customer test still calls it for the wait only (different endpoint).

Follow-up (2026-07-01): NVI `awaitCandidate` timed out at 5m again, and this was a regression from the token cache. `awaitCandidate` captured a single access token once and reused it for the whole multi-minute poll. Before caching, `login()` always minted a fresh ~1h token; after caching, `login()` can return a cached token with as little as ~60s of real life left (served until `expires_in - 60s`). If the wait started with a near-expiry token, it expired mid-poll, every `fetchCandidate` then got 401, `isFullyEvaluated` stayed false, and it timed out; the randomness (cached-token age at wait start) matched the flakiness. Fixed by polling via `fetchCandidateByPublicationId(UIB_NVI_CURATOR, ...)`, which re-requests the token each poll (cached, and auto-refreshed once the cache entry expires), so the request always uses a token with >=60s life. Removed the now-dead single-token `fetchCandidate` helper, `ACCESS_TOKEN_KEY`, and the `CognitoLogin`/`givenAuthenticatedJsonRequest` imports. (Other long-lived loops, `updateApprovalStatus` retry and the search waits, already re-fetch per attempt or are unauthenticated, so this was the only captured-token-in-loop.)

### [x] 3. 409-retry window may be under-provisioned

File: `scientific-index/src/test/java/no/sikt/nva/apitest/scientificindex/candidate/UpdateApprovalStatusApiTest.java:144-156`
Severity: medium-low (flake)

The helper retries transient 409 transaction conflicts caused by trailing re-evaluation events right after creation, but caps at `atMost(30, SECONDS)` while candidate creation waits up to 3 minutes.
Candidates are created back-to-back across 5 tests against shared e2e infra.

Failure: a conflict window longer than 30s surfaces as an Awaitility `ConditionTimeoutException` (hard error) rather than a retried 200.

Fixed (2026-06-29): widened to 120s via `CONFLICT_RETRY_TIMEOUT_SECONDS`. The state-based `awaitCandidate` (see flakiness entry) also means trailing re-evaluation has largely settled before the test mutates, so 409s should be rarer in the first place.

### [ ] 4. `NviCandidate.publicationIdentifier` is never read

File: `scientific-index/src/testFixtures/java/no/sikt/nva/apitest/scientificindex/NviCandidate.java:4`
Severity: low (simplification)

Only `candidateIdentifier()` and `publicationId()` are consumed anywhere; `publicationIdentifier()` is dead state (and `publicationId` is derivable from it as `baseURI + "/publication/" + publicationIdentifier`).
Drop one of the two to avoid the representations drifting.

### [x] 5. `awaitCandidate` fetches the candidate twice

File: `scientific-index/src/testFixtures/java/no/sikt/nva/apitest/scientificindex/NviCandidateFactory.java:57-66`
Severity: low (efficiency)

It polls `fetchCandidate(...).statusCode() == 200` inside the Awaitility predicate, discards that response, then calls `fetchCandidate` again to extract the identifier.
Capture the successful response in an `AtomicReference` (as `updateApprovalStatus` already does) to remove the extra authenticated round-trip and the small TOCTOU re-fetch.

Fixed (2026-06-29): folded into the state-based `awaitCandidate` rewrite (flakiness entry) which captures the settled response in an `AtomicReference` and reads the identifier from it.

### [ ] 6. `updateApprovalStatus`/`putApprovalStatus` + `AtomicReference` hand-rolls "return the last polled value"

File: `scientific-index/src/test/java/no/sikt/nva/apitest/scientificindex/candidate/UpdateApprovalStatusApiTest.java:144-159`
Severity: low (simplification)

Awaitility's `until(callable, predicate)` returns the polled value directly, collapsing the two helpers and the mutable `AtomicReference` into one call and removing the easy mistake of calling `putApprovalStatus` directly and bypassing the 409 retry.

## Dropped after verification (not bugs)

- ISSN-fixture interaction in `BibTexTest.shouldReturnOnlineIssnWhenBothOnlineIssnAndPrintIssnIsPresent`: not broken.
  `createIssnPublication` overrides the channel `id` (271CEF41) and expects `issn={1520-4898}`, which is neither the old nor new fixture value, so the backend resolves ISSN from the channel register by id and ignores the submitted inline `onlineIssn`.
- `period` scalar-vs-object assertion (`FetchCandidateApiTest` report-status vs candidate): appears to reflect the real API response shapes the author observed.
- `401`-vs-`403` for an authenticated curator lacking `MANAGE_NVI`: matches the repo's existing convention (publication-api uses 401).
- `getDouble("totalPoints")` null risk: a real evaluated candidate always has positive points by the time `awaitCandidate` returns 200.
