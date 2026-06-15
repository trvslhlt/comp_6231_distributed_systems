# Web Server

An HTTP web server implementation using the Apache HttpComponents client library.

## Prerequisites

- Java 25+
- Maven

## Build and Run

```bash
mvn clean compile exec:java
```

To specify a port (default: `8080`):

```bash
mvn clean compile exec:java -Dexec.args="<port>"
```

## Package

```bash
mvn clean package
```

## Run Packaged JAR

```bash
java -jar target/web-server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

To specify a port (default: `8080`):

```bash
java -jar target/web-server-1.0-SNAPSHOT-jar-with-dependencies.jar <port>
```
