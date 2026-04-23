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
   ./gradlew build
   ```
   or for Windows:
   ```
   gradlew.bat build
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
./gradlew build -PawsProfile=nva-e2e
./gradlew test -PawsProfile=nva-e2e --rerun-tasks
```

## Generate report

Generate Allure report and open it in the browser:
```
allure generate --open
```

If you have problems viewing the report, try running
```
allure serve allure-results&
```
to start a local webserver for the report.

## License

This project is licensed under the MIT License. See the LICENSE file for details.