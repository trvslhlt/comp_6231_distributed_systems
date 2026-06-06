# Leader Election

A distributed leader election implementation using Apache ZooKeeper.

## Prerequisites

- Java 17+
- Maven
- ZooKeeper running on `localhost:2181`

## Build and Run

```bash
mvn clean compile exec:java
```

## Package

```bash
mvn clean package
```

## Run Packaged JAR

```bash
java -jar target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar
```


