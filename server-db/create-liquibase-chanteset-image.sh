# load env
cd ..
source load-env.sh

cd server-db

set -x

echo SHARES_SERVER_DB_VERSION=${SHARES_SERVER_DB_VERSION}
echo DOCKER_REGISTRY=${DOCKER_REGISTRY}

docker run -d --name kanakartul liquibase/liquibase:4.19 sh

mkdir -p temp/cdoc2
docker cp temp/cdoc2 kanakartul:/liquibase/
rm -rf temp

docker cp ./src/main/resources/db/changelog/db.changelog-master.yaml kanakartul:/liquibase/cdoc2/db.changelog-master.yaml
docker cp ./src/main/resources/db kanakartul:/liquibase/cdoc2
docker cp ./src/main/resources/db/liquibase.properties.docker kanakartul:/liquibase/cdoc2/liquibase.properties

docker commit kanakartul cdoc2-shares-server-liquibase:${SHARES_SERVER_DB_VERSION}

docker stop kanakartul
docker rm kanakartul

export FINAL_REMOTE_URL=${DOCKER_REGISTRY}cdoc2-shares-server/cdoc2-shares-server-liquibase:${SHARES_SERVER_DB_VERSION}
docker tag cdoc2-shares-server-liquibase:${SHARES_SERVER_DB_VERSION} ${FINAL_REMOTE_URL}
docker push ${FINAL_REMOTE_URL}
