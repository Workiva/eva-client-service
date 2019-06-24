# Prepare Final Image
# TODO - We gain a huge start-up performance gain using JDK 9 or above (tested with 10)
# however, JDK12 is the latest to have an alpine image and it is not technically
# in general availability yet until March 2019, we will delay switching until atleast then
# FROM openjdk:12-jre-alpine3.8
FROM openjdk:8-jre-alpine3.8

LABEL authors="Tyler Wilding, Daniel Harasymiw"

RUN apk add --update bash curl libc6-compat nss tomcat-native

COPY ./eva-client-service.jar /opt/eva-client-service.jar
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
