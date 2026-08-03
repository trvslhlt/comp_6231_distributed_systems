# Build context is the assignment_2/ root (not this directory) so the multi-module reactor
# can see all four modules' poms, as required for `mvn -pl ... -am`.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common ./common
COPY service-vehicle-season-price ./service-vehicle-season-price
COPY service-vehicle-total-price ./service-vehicle-total-price
COPY load-balancer ./load-balancer

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -N install \
 && mvn -q -pl load-balancer -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/load-balancer/target/load-balancer.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
