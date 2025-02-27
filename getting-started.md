## Building/Running locally (localhost)

This file describes how to run `cdoc2-shares-server` in your local development machine, without 
external infrastructure.

If you just interested how
to run `cdoc2-shares-server` locally, then see [docker-compose.yml](https://github.com/open-eid/cdoc2-java-ref-impl/tree/master/test/config/shares-server)
in [cdoc2-java-ref-impl](https://github.com/open-eid/cdoc2-java-ref-impl) repo.

### Installing and creating PostgreSQL DB in Docker

#### Install PostgreSQL in Docker
(Docker must be installed)
```bash
docker run --name cdoc2-shares -p 5432:5432 -e POSTGRES_DB=cdoc2-shares -e POSTGRES_PASSWORD=secret -d postgres
docker start cdoc2-shares
```

#### Create DB
From `server-db` directory run:
```bash
cd server-db
mvn liquibase:update
```

### Compiling the servers
From `cdoc2-shares-server` directory run:

```
mvn clean package
```

### Running
(psql in docker must be running)

Two servers instances are needed to be run for sharing the key parts.
From `shares-server` directory run:
```bash
cd shares-server
java -Dspring.config.location=config/application-local.properties -jar target/cdoc2-shares-server-VER.jar
java -Dspring.config.location=config/application-local.properties -jar target/cdoc2-shares-server-VER.jar  --server.port=8442 --management.server.port=18442
```

where VER is the version of the package built by mvn package previously.
Or run servers with `-Dlogging.config=target/test-classes/logback.xml` if you need to see logs.

Note: to enable TLS handshake debugging, add `-Djavax.net.debug=ssl:handshake` option


### cdoc2-shares-server minimal testing

```bash
curl -k https://localhost:18443/actuator/info
```
```json
{"build":{"artifact":"cdoc2-shares-server","name":"cdoc2-shares-server","time":"2025-02-12T12:12:27.587Z","version":"0.4.1-SNAPSHOT","group":"ee.cyber.cdoc2"},"system.time":"2025-02-12T12:22:24Z"}
```
```bash
curl -k https://localhost:18443/actuator/health
```
```json
{"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"livenessState":{"status":"UP"},"readinessState":{"status":"UP"}}}
```

```bash
curl -i -k -X POST https://localhost:8443/key-shares \
-H 'Content-Type: application/json' \
-H 'Accept: application/json' \
-d '{"share":"dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3QK","recipient":"etsi/PNOEE-30303039914"}'
```

```
HTTP/1.1 201 
Location: /key-shares/ee368ad654142dda1d9d8e00744df2c8
```

```
curl -k -X GET https://localhost:8443/key-shares/ee368ad654142dda1d9d8e00744df2c8
```

will give HTTP 401, as GET request requires `x-cdoc2-auth-ticket` and `x-cdoc2-auth-x5c` Header parameters
that are not trivial task to create. 

### cdoc2-shares-server additional testing

Check out [cdoc2-java-ref-impl/cdoc2-cli/README.md](https://github.com/open-eid/cdoc2-java-ref-impl/blob/master/cdoc2-cli/README.md)
for further manual testing (Smart-ID/Mobile-ID)

Alternatively run `cdoc2-shares-server` functional tests from [cdoc2-gatling-tests](https://github.com/open-eid/cdoc2-gatling-tests/tree/master/cdoc2-shares-server)
and/or Smart-ID/Mobile-ID tests from [cdoc2-java-ref-impl/test/bats](https://github.com/open-eid/cdoc2-java-ref-impl/test)
