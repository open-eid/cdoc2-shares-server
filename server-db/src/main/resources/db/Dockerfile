# by default use docker.io as Docker registry, overwrite with
# --build-arg LIQUIBASE_IMAGE=custom.registry.io:8500/liquibase/liquibase
ARG LIQUIBASE_IMAGE=docker.io/liquibase/liquibase:4.29.2
FROM $LIQUIBASE_IMAGE
USER liquibase
WORKDIR /liquibase/changelog

CMD liquibase --url=jdbc:postgresql://cdoc2-shares-postgres:5432/$DB_URL --username=$DB_USER --password=${DB_PASSWORD} update
