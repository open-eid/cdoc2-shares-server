# load env
cd ..
source load-env.sh

cd shares-server

# TODO: should spring-boot.build-image.publish=true only if env variable DOCKER_REGISTRY is set
mvn spring-boot:build-image -Dspring-boot.build-image.publish=false -Dspring-boot.build-image.imageName=${DOCKER_REGISTRY}cdoc2-shares-server/shares-server:${SHARES_SERVER_VERSION} -Dspring-boot.build-image.createdDate=now
