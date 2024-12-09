#!/usr/bin/env bash
# build Docker image locally
#set -x

SHARES_SERVER_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
DOCKER_REGISTRY=ghcr.io
DOCKER_REPOSITORY=open-eid
IMAGE_NAME=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)

LIQUIBASE_IMAGE_NAME=cdoc2-shares-server-liquibase

mvn spring-boot:build-image \
-Dmaven.test.skip=true \
-Dspring-boot.build-image.publish=false \
-Dspring-boot.build-image.imageName=${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${IMAGE_NAME}:${SHARES_SERVER_VERSION} \
-Dspring-boot.build-image.tags=${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${IMAGE_NAME}:latest \
-Dspring-boot.build-image.createdDate=now
