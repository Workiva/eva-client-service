####
# Docker Commands
gen-docker:
	docker build \
		-f workivabuild.Dockerfile \
		-t drydock.workiva.net/workiva/eva-client-service:latest-release .

gen-docker-no-tests:
	docker build \
		--build-arg SKIP_TESTS=true \
		-f workivabuild.Dockerfile \
		-t drydock.workiva.net/workiva/eva-client-service:latest-release .

run-docker:
	docker-compose -f docker-compose.yml \
		-f docker-compose.override.yml up -d

stop-docker:
	docker-compose -f docker-compose.yml \
		-f docker-compose.override.yml down

docker-logs:
	docker-compose -f docker-compose.yml \
		-f docker-compose.override.yml logs

####
# Maven Installation
####
install:
	mvn -DskipTests=true -Dcheckstyle.skip install

####
# Application Execution Recipes
####
debug-local: install ## Run with DEBUG log level
    ECS_LOG_PARAMS=true \
    SANITIZE_EXCEPTIONS=false \
    LOGBACK_APPENDER=STDOUT \
    LOGBACK_LOG_LEVEL=DEBUG \
    java -jar target/client-service*.jar -disableTelemetry=true --server.port=8080

run-local: install ## Runs the built emitter locally, with local test settings
    SANITIZE_EXCEPTIONS=false \
    LOGBACK_APPENDER=STDOUT \
    java -jar target/client-service*.jar -disableTelemetry=true --server.port=8081

traced-local: install
	SANITIZE_EXCEPTIONS=false \
	LOGBACK_APPENDER=STDOUT \
	java -jar target/client-service*.jar -disableTelemetry=true --server.port=8080 -tracing=jaeger

catalog-local: install
    SANITIZE_EXCEPTIONS=false \
    LOGBACK_APPENDER=STDOUT \
    java -jar target/client-service*.jar -disableTelemetry=true --server.port=8080 --eva.catalog=http://localhost:3000

####
# Linting and Test Recipes
####
coverage:  ## Run unit tests with coverage
	mvn -Dcheckstyle.skip clean verify
	open target/site/jacoco/index.html

fmt:
	mvn fmt:format

lint:  ## Check for style guide violations in codebase
	mvn checkstyle:check

skynet-local:
	newman run postman/colls/EVA_101.postman_collection.json -e postman/envs/EVA_Local.postman_environment.json

test:
	mvn -Dcheckstyle.skip test

update-tocs:
	./.circleci/scripts/update-tocs.sh
