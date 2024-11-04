## Running locally (localhost)

This file describes how to run cdoc2 key shares servers in your local development machine, without 
external infrastructure

### Installing and creating PostgreSQL DB in Docker

#### Install PostgreSQL in Docker
(Docker must be installed)
```
docker run --name cdoc2-psql -p 5432:5432 -e POSTGRES_DB=cdoc2 -e POSTGRES_PASSWORD=secret -d postgres
docker start cdoc2-psql
```

#### Create DB
From server-db directory run:
```
mvn liquibase:update
```

### Compiling the servers
From cdoc2-shares-server directory run:
```
mvn clean package
```

### Running
(psql in docker must be running)

Two servers instances are needed to be run for sharing the key parts.
From server directory run:
```
java -Dspring.config.location=config/application-local.properties -jar target/shares-server-VER.jar
java -Dspring.config.location=config/application-local.properties -jar target/shares-server-VER.jar  --server.port=8442 --management.server.port=18442
```

where VER is the version of the package built by mvn package previously.
Or run servers with `-Dlogging.config=target/test-classes/logback.xml` if you need to see logs.

Note: to enable TLS handshake debugging, add `-Djavax.net.debug=ssl:handshake` option
