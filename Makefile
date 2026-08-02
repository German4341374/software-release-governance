SHELL := /bin/sh

.PHONY: setup test verify build up down logs clean

setup:
	cp .env.example .env
	./mvnw --batch-mode --no-transfer-progress dependency:go-offline

test:
	./mvnw --batch-mode --no-transfer-progress test

verify:
	./mvnw --batch-mode --no-transfer-progress verify

build:
	./mvnw --batch-mode --no-transfer-progress clean package

up:
	docker compose up --build --detach --wait

down:
	docker compose down

logs:
	docker compose logs --follow application

clean:
	./mvnw clean
	docker compose down --volumes --remove-orphans
