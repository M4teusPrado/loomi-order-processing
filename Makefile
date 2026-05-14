.PHONY: up down build test logs clean setup

setup: build

up:
	docker-compose up --build -d

down:
	docker-compose down

build:
	./mvnw clean package -DskipTests

test:
	./mvnw test

logs:
	docker-compose logs -f app

clean:
	docker-compose down -v
	./mvnw clean
