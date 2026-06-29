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

### [ ] 3. 409-retry window may be under-provisioned

File: `scientific-index/src/test/java/no/sikt/nva/apitest/scientificindex/candidate/UpdateApprovalStatusApiTest.java:144-156`
Severity: medium-low (flake)

The helper retries transient 409 transaction conflicts caused by trailing re-evaluation events right after creation, but caps at `atMost(30, SECONDS)` while candidate creation waits up to 3 minutes.
Candidates are created back-to-back across 5 tests against shared e2e infra.

Failure: a conflict window longer than 30s surfaces as an Awaitility `ConditionTimeoutException` (hard error) rather than a retried 200.

### [ ] 4. `NviCandidate.publicationIdentifier` is never read

File: `scientific-index/src/testFixtures/java/no/sikt/nva/apitest/scientificindex/NviCandidate.java:4`
Severity: low (simplification)

Only `candidateIdentifier()` and `publicationId()` are consumed anywhere; `publicationIdentifier()` is dead state (and `publicationId` is derivable from it as `baseURI + "/publication/" + publicationIdentifier`).
Drop one of the two to avoid the representations drifting.

### [ ] 5. `awaitCandidate` fetches the candidate twice

File: `scientific-index/src/testFixtures/java/no/sikt/nva/apitest/scientificindex/NviCandidateFactory.java:57-66`
Severity: low (efficiency)

It polls `fetchCandidate(...).statusCode() == 200` inside the Awaitility predicate, discards that response, then calls `fetchCandidate` again to extract the identifier.
Capture the successful response in an `AtomicReference` (as `updateApprovalStatus` already does) to remove the extra authenticated round-trip and the small TOCTOU re-fetch.

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
