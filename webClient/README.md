# Web Client

An HTTP client for communicating with the Web Server, using the Apache HttpComponents client library.

## Prerequisites

- Java 25+
- Maven
- Web Server running on `localhost:8080`

## Build and Run

```bash
mvn clean compile exec:java
```

To specify a server address (default: `http://localhost:8080`):

```bash
mvn clean compile exec:java
```

## Package

```bash
mvn clean package
```

## Run Packaged JAR

```bash
java -jar target/web-client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

To specify a server address (default: `http://localhost:8080`):

```bash
java -jar target/web-client-1.0-SNAPSHOT-jar-with-dependencies.jar <serverAddress>
```
