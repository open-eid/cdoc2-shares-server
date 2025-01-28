#!/usr/bin/env bash

#set -x

SHARES_SERVER_VERSION=$(cd ../shares-server && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
DOCKER_REGISTRY=ghcr.io
DOCKER_REPOSITORY=open-eid

LIQUIBASE_IMAGE_NAME=cdoc2-shares-server-liquibase

# version shows what version of shares-server is used in pair with liquibase image
# Docker version should be same as shares-server-version although server-db pom version might be different
docker build -t ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${LIQUIBASE_IMAGE_NAME}:${SHARES_SERVER_VERSION} ../server-db/src/main/resources/db
docker tag ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${LIQUIBASE_IMAGE_NAME}:${SHARES_SERVER_VERSION}  ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${LIQUIBASE_IMAGE_NAME}:latest
