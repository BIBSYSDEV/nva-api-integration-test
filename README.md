# api-test

This project is a simple API testing application built using Java and Gradle.

## Project Structure

```
api-test
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle
│   └── wrapper
│       └── gradle-wrapper.properties
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── App.java
│   │   └── resources
│   │       └── application.properties
│   └── test
│       ├── java
│       │   └── com
│       │       └── example
│       │           └── AppTest.java
│       └── resources
├── .gitignore
└── README.md
```

## Setup Instructions

1. Clone the repository:
   ```
   git clone <repository-url>
   ```

2. Navigate to the project directory:
   ```
   cd api-test
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

## License

This project is licensed under the MIT License. See the LICENSE file for details.