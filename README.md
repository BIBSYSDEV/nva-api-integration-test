# api-test

This project is a simple API testing application built using Java and Gradle.

## Setup instructions

1. Clone the repository:

   ```
   git clone <repository-url>
   ```

2. Navigate to the project directory:

   ```
   cd nva-api-integration-test
   ```

3. Run the Gradle wrapper to build the project:
   ```
   ./gradlew build -x test
   ```
   or for Windows:
   ```
   gradlew.bat build -x test
   ```
4. Install Allure for reports:
   ```
   npm install -g allure
   ```

## Usage

This assumes you have configured AWS CLI with an SSO session named `sikt` and an account profile named `nva-e2e`.

Log in once per session (the SSO token is reused across profiles under the `sikt` session):

```
aws sso login --sso-session sikt
```

Then pass the profile to any Gradle task via `-PawsProfile=<profile>`:

```
# Run a full build with tests
./gradlew build -PawsProfile=nva-e2e

# Force re-run of all tests
./gradlew test -PawsProfile=nva-e2e --rerun-tasks
```

## Generate report

Generate the Allure report from local test results and open it in the browser:

```
allure generate "*/build/allure-results" && open allure-report/index.html
```

Note: The `--open` flag in Allure is broken (as of `3.14.3`), so the report must be opened separately.

Report settings live in `allurerc.yaml`: single-file output, the history file location, and how many past runs to keep (`historyLimit`).
Every generation appends an entry to `allure-history.jsonl`, so the report shows a trend chart and per-test history across your recent runs.
Delete `allure-history.jsonl` to reset the local history.

The CI pipeline running automated tests uses the same configuration and maintains a history file in S3.
Each run downloads `allure-history.jsonl` from the report bucket, updates it, and then re-uploads it so that history is carried across executions.
Dated single-file reports are also archived under `reports/` in the bucket.

## License

This project is licensed under the MIT License. See the LICENSE file for details.
