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

create-schema:
	cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/runner.yml build flyway
	export POSTGRES_HOST=$${POSTGRES_HOST:-postgres}
	export POSTGRES_PORT=$${POSTGRES_PORT:-5432}
	export POSTGRES_USER=$${POSTGRES_USER:-talismane}
	export POSTGRES_PASSWORD=$${POSTGRES_PASSWORD:-talismanepassword}
	POSTGRES_DATABASE="terms-test" docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/runner.yml run -T flyway
	POSTGRES_DATABASE="terms" docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/runner.yml run -T flyway

clean-docker-compose:
	cd "${CURDIR}"
	docker-compose -p $(COMPOSE_PROJECT) -f docker-compose/deps.yml -f docker-compose/runner.yml down --remove-orphans


