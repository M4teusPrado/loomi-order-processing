.PHONY: up down build test logs clean setup db-migrate

setup: build

up:
	docker-compose up --build -d

down:
	docker-compose down

build:
	mvn clean package -DskipTests

test:
	mvn test

test-integration:
	mvn verify

logs:
	docker-compose logs -f app

db-migrate:
	mvn flyway:migrate -Dspring-boot.run.profiles=dev

clean:
	docker-compose down -v
	mvn clean
