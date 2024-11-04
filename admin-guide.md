# CDOC2 Key Shares Server Administration Guide

This document describes how to configure and run CDOC2 key shares servers.

## Database

The key capsule server requires a pre-installed PostgreSQL database to store data.

### Configuration

The creation and updating of the database schema is currently done from the source tree
using `liquibase-maven-plugin`. In order to create or update the database schema
Maven (at least 3.8.4) and Java (at least JDK 17) are required.

In `server-db/liquibase.properties` configure the database connection parameters to match your PostgreSQL
installation:

```
url: jdbc:postgresql://HOST/DB_NAME
username: postgres
password: secret
```

### Create and Update

To create or update the CDOC2 key shares server's database run the following command from `server-db` folder:

`
mvn liquibase:update
`

## Server

The CDOC2 Key Shares Server backend requires to run few server instances for sending and reading 
key shares capsules.

### Keystore Creation

The servers require a keystore file to secure HTTP connections using TLS.

The keystore file is created with the `keytool` utility
(included with Java Runtime).

To generate a keystore file `cdoc2server.p12` with password `passwd`, alias `cdoc2-server` and validity of 3650 days:
```
keytool -genkeypair -alias cdoc2-server -keyalg ec -groupname secp384r1 -sigalg SHA512withECDSA -keystore cdoc2server.p12 -storepass passwd -validity 3650
```

For more details about operations with certificates in keystore files, see [^1].

### Key Shares Server

#### Requirements
- Java runtime (at least JDK 17)
- the application binary `shares-server-<VERSION>.jar`
- the configuration file `application.properties`

#### Configuration

The configuration file `application.properties` must contain the following configuration parameters:

```
# The format used for the keystore. It could be set to JKS in case it is a JKS file
server.ssl.key-store-type=PKCS12

# The path to the keystore containing the certificate
server.ssl.key-store=/path/to/cdoc2server.p12

# The keystore password to access its entries
server.ssl.key-store-password=passwd

# The alias mapped to the certificate in the keystore
server.ssl.key-alias=cdoc2-server

# Enable server TLS
server.ssl.enabled=true

# allow only TLSv1.3
server.ssl.enabled-protocols=TLSv1.3

# The port the server is started on
server.port=8443

# Database configuration
spring.datasource.url=jdbc:postgresql://HOST/DB_NAME
spring.datasource.username=postgres
spring.datasource.password=secret
spring.datasource.driver-class-name=org.postgresql.Driver

# logging levels
# change to 'debug' if you want to see logs. Run server with -Dlogging.config=target/test-classes/logback.xml
logging.level.root=info
logging.level.ee.cyber.cdoc2=trace
```

#### Running

To run the server, execute the following command:

`
java -jar -Dspring.config.location=application.properties shares-server-VER.jar
`

#### Running in Docker

From `1.4.1` release docker images are using `Java 21` as Java base image. To fully use Java21 improvements
enable virtual threads by adding `spring.threads.virtual.enabled=true` into `application.properties`.

To get better throughput it's recommended to give at least 2 CPUs and 1GB of memory per `server` instance.
2 GB of memory per process is even better (some additional throughput gains).

Instead of creating two VMs with 1CPU, create single VM with 2 CPU and run both `server` on that 
VM instance (two Java process per VM sharing 2 CPUs).

For sample setup see:
* [cdoc2-gatling-tests/setup-load-testing](https://github.com/open-eid/cdoc2-gatling-tests/tree/master/setup-load-testing) for `docker run` examples
* [cdoc2-java-ref-impl/test/config/server/docker-compose.yml](https://github.com/open-eid/cdoc2-java-ref-impl/blob/master/test/config/server/docker-compose.yml) for `docker compose` example


## Monitoring

To enable standard Spring monitoring endpoints, `application.properties` must contain following lines:
```
# https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.monitoring
# run management on separate port
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

# credentials for /actuator/prometheus api basic authentication
management.endpoints.metrics.username=username
management.endpoints.metrics.password=password

# configure monitoring endpoints
management.endpoints.enabled-by-default=false
management.endpoints.web.discovery.enabled=false
# key values can be hidden when change value to "never" or "when_authorized"
management.endpoint.env.show-values=always
management.endpoint.configprops.show-values=always

# explicitly enable endpoints
management.endpoint.info.enabled=true
management.endpoint.health.enabled=true
management.endpoint.health.show-details=always
management.endpoint.startup.enabled=true

# expose endpoints
management.endpoints.web.exposure.include=info,health,startup,prometheus

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
```

If needed to list all spring boot application.properties enable endpoint `/env` temporary:
```
management.endpoints.web.exposure.include=info,health,startup,prometheus,env
management.endpoint.env.enabled=true
```
Endpoint output can see in `https://<management_host>:<management_port>/actuator/env`


NB! Currently, the monitoring endpoints require no authentication. As these endpoints are
running on a separate HTTP port, the access to the monitoring endpoints must be implemented by network access rules (e.g firewall).
Only `/prometheus` endpoint is authenticated.


### Info endpoint 
`curl -k -X GET https://<management_host>:<management_port>/actuator/info`

```json
{
  "build": {
    "artifact": "shares-server",
    "name": "shares-server",
    "time": "2023-01-17T14:31:18.918Z",
    "version": "0.1.0-SNAPSHOT",
    "group": "ee.cyber.cdoc2"
  },
  "system.time": "2023-01-17T14:48:39Z"
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
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499596230656,
        "free": 415045992448,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Startup endpoint
`curl -k -X GET https://<management_host>:<management_port>/actuator/startup`

```json
{
  "springBootVersion": "3.2.5",
  "timeline": {
    "startTime": "2023-01-17T14:36:17.935227352Z",
    "events": []
  }
}
```

### Prometheus endpoint (authentication required)
`curl -k -u <username>:<password> https://<management_host>:<management_port>/actuator/prometheus -X GET`

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


[^1]: https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG182