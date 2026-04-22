# api-test

This project is a simple API testing application built using Java and Gradle.

## Setup Instructions

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

## Usage

Before running the application run
```
. login.bash <aws_profile>
```
to set up aws credentials

```
npm install -g allure
```
to set up allure report

To run the application, use the following command:
```
./gradlew run
```
or for Windows:
```
gradlew.bat run
```

## Testing

To run the tests, execute:
```
./gradlew test
```
or for Windows:
```
gradlew.bat test
```

## Generate report

Run
```
allure generate --open
```
to generate allure report

If you have problems viewing the report, try running
```
allure serve allure-results&
```
to start a local webserver for the report.

## License

This project is licensed under the MIT License. See the LICENSE file for details.