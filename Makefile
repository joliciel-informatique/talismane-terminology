COMPOSE_PROJECT ?= "TALISMANE_TERMINOLOGY"

.PHONY: test start-dep stop-dep
.ONESHELL:

test: start-dep
	@ cd "${CURDIR}"
	mvn test

start-dep:
	@ cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/deps.yml build
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/deps.yml up -d

stop-dep:
	@ cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/deps.yml down

erase-dep-state: stop-dep
	@ cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/deps.yml rm

export POSTGRES_HOST=postgres
export POSTGRES_PORT=5432
export POSTGRES_USER=talismane
export POSTGRES_PASSWORD=password
export POSTGRES_DATABASE=terms

create-schema:
	@ cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/runner.yml build flyway
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/runner.yml run -T flyway

create-test-schema:
	export POSTGRES_DATABASE=terms-test
	@ cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/runner.yml build flyway
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/runner.yml run -T flyway

clean-docker-compose:
	cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/deps.yml -f docker-compose/runner.yml down --remove-orphans


