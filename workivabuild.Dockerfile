# Initial Setup
FROM maven:3.6-jdk-8-alpine as setup

# Vendor compose file for local/testing consumption
COPY ./docker-compose.yml /docker-compose.yml
ARG BUILD_ARTIFACTS_COMPOSE=/docker-compose.yml

## Setup Maven Authentication
RUN mkdir -p /root/.m2

## Pre-fetch Dependencies
WORKDIR /prefetch
COPY ./pom.xml /prefetch/pom.xml
ENV MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
RUN mvn -B dependency:go-offline -q

# Lint Documentation
FROM node:11.6.0-alpine as lint

WORKDIR /build
COPY . /build

RUN apk add --update bash
RUN npm install
RUN ./.circleci/scripts/verify-docs.sh

# Build Project
FROM maven:3.6-jdk-8-alpine as build

WORKDIR /build
COPY . /build
ENV MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"

## Grab Pre-fetched Dependencies
COPY --from=setup /root/.m2 /root/.m2
ARG SKIP_TESTS
RUN ./scripts/ci/verify.sh

## Linter Processes
RUN mvn -B fmt:check -q
RUN mvn -B checkstyle:checkstyle -q

## Dependency Artifacts
RUN apk add --update git perl
RUN ./scripts/ci/dependencies.sh
RUN mkdir -p /audit
RUN cp ./MANIFEST.yml /audit/MANIFEST.yml
RUN cp ./pom.xml /audit/pom.xml
ARG BUILD_ARTIFACTS_AUDIT=/audit/*

## Swagger Documentation
RUN cd ./docs/swagger \
 && tar -zcvf ../../eva-client-service-api-docs.tar.gz .
ARG BUILD_ARTIFACTS_DOCUMENTATION=/build/eva-client-service-api-docs.tar.gz

## Upload Code-Coverage Report
ARG BUILD_ARTIFACTS_TEST_REPORTS=/build/target/surefire-reports/TEST-*.xml

## Produce Final JAR, AOT Compile EVA
RUN ./scripts/ci/aot-compile.sh
RUN mvn package -DskipTests
RUN mv ./target/*.jar ./eva-client-service.jar

## Veracode Artifact
RUN tar czf ./java.tar.gz ./eva-client-service.jar
ARG BUILD_ARTIFACTS_VERACODE=/build/java.tar.gz

# Prepare Final Image
# TODO - We gain a huge start-up performance gain using JDK 9 or above (tested with 10)
# however, JDK12 is the latest to have an alpine image and it is not technically
# in general availability yet until March 2019, we will delay switching until atleast then
# FROM openjdk:12-jre-alpine3.8
FROM openjdk:8-jre-alpine3.8

LABEL authors="Tyler Wilding, Daniel Harasymiw"

RUN apk add --update bash curl libc6-compat nss tomcat-native

COPY --from=build /build/eva-client-service.jar /opt/eva-client-service.jar
COPY ./scripts/image/set-mem-constraints.sh /usr/local/bin/set-mem-constraints.sh
COPY ./scripts/image/run-service.sh /usr/local/bin/run-service.sh

RUN chmod o+rx /opt/eva-client-service.jar
RUN chmod o+rx /usr/local/bin/set-mem-constraints.sh
RUN chmod o+rx /usr/local/bin/run-service.sh

## Update Packages for Security Updates
ARG BUILD_ID
RUN apk update && apk upgrade

## Default options, which are interval 30s, timeout 30s, start-period 0s, and retries 3.
HEALTHCHECK CMD curl --fail http://localhost:8080/actuator/health || exit 1
USER nobody
CMD [ "sh", "/usr/local/bin/run-service.sh" ]
