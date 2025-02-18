# CDOC2 Key Shares Server Administration Guide

This document describes how to configure and run `cdoc2-shares-server`.

## Running

To run without Docker, see [getting-started.md](getting-started.md)

### Running in Docker

Docker images are using `Java 21` as Java base image. To fully use Java 21 improvements
enable virtual threads by adding `spring.threads.virtual.enabled=true` into `application.properties`.

To get better throughput it's recommended to give at least 2 CPUs and 1GB of memory per `server` instance.
2 GB of memory per process is even better (some additional throughput gains).

In nutshell run `cdoc2-shares-server`:

```bash
docker run -v /path/to/config/application.properties:/app/config/application.properties \
    -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/application.properties \
    ghcr.io/open-eid/cdoc2-shares-server:latest
```

Sample `application.properties` files are in [shares-server/config](shares-server/config)

For full sample setups using `docker compose` and `docker run` see:
* [cdoc2-gatling-tests/setup-load-testing](https://github.com/open-eid/cdoc2-gatling-tests/) for `docker run` examples (TODO: update links)
* [cdoc2-java-ref-impl/test/config/shares-server/docker-compose.yml](https://github.com/open-eid/cdoc2-java-ref-impl/tree/SID/test/config/shares-server) for `docker compose` example ((TODO: update links))


## Database

The cdoc2-shares-server requires a pre-installed PostgreSQL database to store data.

### DB creation

The creation and updating of the database schema is easiest to complete with prebuilt `cdoc2-shares-server-liquibase` 
Docker image:

```bash
docker run --rm \
  --env DB_URL=jdbc:postgresql://cdoc2-shares-psql/cdoc2-shares \
  --env DB_PASSWORD=secret \
  --env DB_USER=postgres \
  ghcr.io/open-eid/cdoc2-shares-server-liquibase:latest
```

More info in [postgres.README.md](postgres.README.md).

Alternatively create/update DB with `mvn liquibase:update`, see [getting_started.md](getting-started.md)
for more info.

### DB configuration

`application.properties`:
```
# Database configuration
spring.datasource.url=jdbc:postgresql://cdoc2-shares-psql/cdoc2-shares
spring.datasource.username=postgres
spring.datasource.password=secret
spring.datasource.driver-class-name=org.postgresql.Driver
```

## TLS setup

Instruction for acquiring production TLS certificates is out of scope for this document, but test
certificates and private keys can be generated as:

### TLS Keystore Creation

The servers require a keystore file to secure HTTP connections using TLS.

The keystore file is created with the `keytool` utility
(included with Java Runtime).

To generate a keystore file `cdoc2server.p12` with password `passwd`, alias `cdoc2-server` and validity of 3650 days:
```
keytool -genkeypair -alias cdoc2-server -keyalg ec -groupname secp384r1 -sigalg SHA512withECDSA -keystore cdoc2server.p12 -storepass passwd -validity 3650
```

### TLS configuration
Server TLS certificates/private keys are configured in `application.properties`:
```
server.ssl.key-store-type=PKCS12
# The path to the keystore containing the certificate
server.ssl.key-store=../keys/cdoc2server.p12
# The password used to generate the certificate
server.ssl.key-store-password=passwd
# The alias mapped to the certificate
server.ssl.key-alias=cdoc2-server

server.ssl.enabled=true
# enable TLSv1.3 only
server.ssl.enabled-protocols=TLSv1.3
```

## Trusted  auth token certificate issuers

When key shares are downloaded, then downloader must provide his/her certificate in `x-cdoc2-auth-x5c`
[HTTP header parameter](https://github.com/open-eid/cdoc2-openapi/blob/55a0b02adae0d8c61f2589a47555a93e4cf31971/cdoc2-key-shares-openapi.yaml#L54C17-L54C33). 
These certificates are only accepted, when they are issued by 
trusted certificate issuers. `x-cdoc2-auth-x5c` trusted issuers are configured through 
`application.properties`:
```
# Enable/disable certificate revocation checking for auth ticket certificates using
# Certificate revocation URL is in certificate AIA extension using OCSP.
# When certificate doesn't have AIA extension and revocation checks are enabled, then authentication
# will fail
cdoc2.auth-x5c.revocation-checks.enabled=false

# https://docs.spring.io/spring-boot/reference/features/ssl.html#features.ssl.pem
# Smart-ID/Mobile-ID certificate trusted issuer
spring.ssl.bundle.jks.sid-trusted-issuers.truststore.location=src/test/resources/sid-trusted-issuers/test_sid_trusted_issuers.jks
spring.ssl.bundle.jks.sid-trusted-issuers.truststore.password=changeit
spring.ssl.bundle.jks.sid-trusted-issuers.truststore.type=jks
```

### Creating auth token certificate trusted issuers truststore

Maintaining trusted certificate issuers is out of scope of this document (depends on other infra),
but for testing purposes [create-truststore_with_certs.sh](shares-server/src/test/resources/sid-trusted-issuers/create-truststore_with_certs.sh)
script generates truststore for auth token issuers.

## Nonce expiry and clean-up

`application.properties`:
```
# nonce validity time in seconds, default 300
cdoc2.nonce.expiration.seconds=300

# Expired share nonce removing job executes every 5 minutes every day
key-share-nonce.expired.clean-up.cron=0 0/5 * * * ?
```

## Logging

`cdoc2-shares-server` uses following lines to configure logging from `application.properties`:
```
logging.level.root=info
logging.level.ee.cyber.cdoc2=trace
 ```

When running in Docker, then logs can be viewed `docker logs <containerID>` command or 
from file `/var/lib/docker/containers/<container-id>/<container-id>-json.log`.

Internally Spring Boot uses [logback](https://logback.qos.ch/). More advanced logging can be set up
by configuring logback through configuration file. Logback configuration location can be configured
by setting `LOGGING_CONFIG` environment variable or passing `logging.config` as Java system property:

```bash
docker run -v /path/to/config/logback.xml:/app/config/logback.xml \
    -v /path/to/config/application.properties:/app/config/application.properties \
    -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/application.properties \
    -e LOGGING_CONFIG=/app/config/logback.xml \
    ghcr.io/open-eid/cdoc2-shares-server:latest
```

Example [logback.xml](src/test/resources/logback.xml)

More info on setting up Spring Boot logging: <https://docs.spring.io/spring-boot/reference/features/logging.html>

## Monitoring and metrics

By default `cdoc2-shares-server` exposes `info`, `health`, `startup` and `prometheus` monitoring endpoints.

NB! Currently, the monitoring endpoints require no authentication. As these endpoints are
running on a separate HTTPS port, the access to the monitoring endpoints must be implemented by network access rules (e.g firewall).
Only `/prometheus` endpoint is authenticated.


### Info endpoint
`curl -k -X GET https://<management_host>:<management_port>/actuator/info`

```json
{
  "build": {
    "artifact": "cdoc2-shares-server",
    "name": "cdoc2-shares-server",
    "time": "2025-02-12T13:54:45.711Z",
    "version": "0.4.1-SNAPSHOT",
    "group": "ee.cyber.cdoc2"
  },
  "system.time": "2025-02-14T09:46:03Z"
}
```

### Health endpoint
`curl -k -X GET https://<management_host>:<management_port>/actuator/health`

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

### Startup endpoint
`curl -k -X GET https://<management_host>:<management_port>/actuator/startup`

```json
{
  "timeline": {
    "startTime": "2025-02-14T09:48:09.832145831Z",
    "events": []
  },
  "springBootVersion": "3.4.1"
}
```

### Metrics - Prometheus endpoint (authentication required)
`curl -k -u <username>:<password> https://<management_host>:<management_port>/actuator/prometheus -X GET`

(username and password are defined in application.properties as `username` and `password` under `management.endpoints.metrics`)

```
# HELP executor_pool_core_threads The core number of threads for the pool
# TYPE executor_pool_core_threads gauge
executor_pool_core_threads{application="shares-server",applications="transfer-service",name="applicationTaskExecutor",} 8.0
# HELP jvm_memory_usage_after_gc_percent The percentage of long-lived heap pool used after the last GC event, in the range [0..1]
# TYPE jvm_memory_usage_after_gc_percent gauge
jvm_memory_usage_after_gc_percent{application="shares-server",applications="transfer-service",area="heap",pool="long-lived",} 0.00239985994123664
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="shares-server",applications="transfer-service",area="nonheap",id="Compressed Class Space",} 1.0815688E7
...........
# HELP jdbc_connections_active Current number of active connections that have been allocated from the data source.
# TYPE jdbc_connections_active gauge
...........
```

Prometheus endpoint just provides monitoring data, usually its visualized with some other tool, like [Grafana](https://grafana.com/grafana/)


### Monitoring/metrics configuration

Configuring metrics is out of scope for this document, official Spring Boot Documentation:
<https://docs.spring.io/spring-boot/reference/actuator/metrics.html#actuator.metrics.supported>

Sample `application.properties`:
```
# https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.monitoring
# run management on separate https port
management.server.port=18443
management.server.ssl.enabled=true
management.server.ssl.key-store-type=PKCS12
# The path to the keystore containing the certificate
# See copy-keys-and-certificates in pom.xml
management.server.ssl.key-store=../keys/cdoc2server.p12
# The password used to generate the certificate
management.server.ssl.key-store-password=passwd
# The alias mapped to the certificate
management.server.ssl.key-alias=cdoc2-server

# configure monitoring endpoints
management.endpoints.enabled-by-default=false
management.endpoints.web.discovery.enabled=false
# key values can be hidden when change value to "never" or "when_authorized"
management.endpoint.env.show-values=always
management.endpoint.configprops.show-values=always

# explicitly enable endpoints
management.endpoint.info.enabled=true
management.endpoint.health.enabled=true
management.endpoint.startup.enabled=true
management.endpoint.prometheus.enabled=true
management.endpoint.health.show-details=always
management.endpoint.env.enabled=false

# expose only liveness, readiness and database indicators for /actuator/health endpoint
management.health.defaults.enabled=false
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
management.health.db.enabled=true

# expose endpoints
management.endpoints.web.exposure.include=info,health,startup,prometheus,env

# Supported metrics
# https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics.supported

#enable tomcat.* metrics
server.tomcat.mbeanregistry.enabled=true

# Spring Data Repository Metrics
# https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics.supported.spring-data-repository
# spring.data.repository.* metrics
management.metrics.data.repository.autotime.enabled=true

# https://docs.spring.io/spring-boot/docs/2.1.5.RELEASE/reference/htmlsingle/#production-ready-metrics-spring-mvc
# http.server.requests metrics
management.metrics.web.server.auto-time-requests=true

# access security must be implemented at network access rules (firewall)
management.security.enabled=false
endpoints.health.sensitive=false

# credentials for /actuator/prometheus api basic authentication
management.endpoints.metrics.username=username
management.endpoints.metrics.password=password
```

[^1]: https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG182