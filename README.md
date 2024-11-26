# CDOC2 Key Shares Server

CDOC2 Key Shares Server for [CDOC2](https://open-eid.github.io/CDOC2/). 

Implements `cdoc2-key-shares-openapi` [OpenAPI spec](https://github.com/open-eid/cdoc2-openapi/blob/master/cdoc2-key-shares-openapi.yaml) from [cdoc2-openapi](https://github.com/open-eid/cdoc2-openapi/)
for Key Shares upload/download. Used by [cdoc2-java-ref-impl](https://github.com/open-eid/cdoc2-java-ref-impl) 
and [DigiDoc4-Client](https://github.com/open-eid/DigiDoc4-Client) for CDOC2 encryption/decryption server scenarios.

## Structure

  - server              - Implements `/key-shares` API-s. 
  - server-db           - shared DB code. Liquibase based DB creation
  - server-openapi      - server stub generation from OpenAPI specifications
  - cdoc2-shared-crypto - some shared crypto functions

## Preconditions for building
* Java 17
* Maven 3.8.x
* Docker available and running (required for running tests, use `-Dmaven.test.skip=true` to skip)

## Maven dependencies

Depends on:
* https://github.com/open-eid/cdoc2-openapi OpenAPI specifications for server stub generation
* https://github.com/open-eid/cdoc2-java-ref-impl (for tests only, use `-Dmaven.test.skip=true` to skip)

Configure github package repo access
https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token

Example `<profile>` section of `settings.xml` for using cdoc2 dependencies:
```xml
  <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
        </repository>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/open-eid/cdoc2-java-ref-impl</url>
        </repository>
      </repositories>
  </profile>
```

Note: When pulling, the package index is based on the organization level, not the repository level.
https://stackoverflow.com/questions/63041402/github-packages-single-maven-repository-for-github-organization

So defining single Maven package repo from `open-eid` is enough for pulling cdoc2-* dependencies.

## Building & Running

[![Build cdoc2-shares-server with CI](https://github.com/open-eid/cdoc2-shares-server/actions/workflows/maven.yml/badge.svg)](https://github.com/open-eid/cdoc2-shares-server/actions/workflows/maven.yml)

```bash
mvn clean install
```

### GitHub workflow build

Maven build is executed for GH event `pull_request` an and `push` to 'master'.

GH build workflow configures Maven repository automatically. For fork based pull_requests
Maven repo value will be set to `github.event.pull_request.base.repo.full_name` (`open-eid/*`). It can be overwritten
by [defining repository variable](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/variables#creating-configuration-variables-for-a-repository)
`MAVEN_REPO`


### Running

See [getting-started.md](getting-started.md) and [admin-guide.md](admin-guide.md)

### Running pre-built Docker/OCI images

Download `cdoc2-shares-server` image from [open-eid Container registry](https://github.com/orgs/open-eid/packages?ecosystem=container)

* See [cdoc2-gatling-tests/setup-load-testing](https://github.com/open-eid/cdoc2-gatling-tests/tree/master/setup-load-testing) for `docker run` examples 
* See [cdoc2-java-ref-impl/test/config/server/docker-compose.yml](https://github.com/open-eid/cdoc2-java-ref-impl/blob/master/test/config/server/docker-compose.yml) for `docker compose` example

To create `cdoc2` database required by `server` see [postgres.README.md](postgres.README.md)

## Releasing and versioning

See [VERSIONING.md](https://github.com/open-eid/cdoc2-java-ref-impl/blob/master/VERSIONING.md)

### GitHub release

[Create release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository#creating-a-release) on tag done by [VERSIONING.md](https://github.com/open-eid/cdoc2-java-ref-impl/blob/master/VERSIONING.md) process. 
It will trigger `maven-release.yml` workflow that will deploy Maven packages to GitHub Maven package repository
and build & publish Docker/OCI images.


## Related projects

* Gatling tests (load and functional) for cdoc2-shares-server https://github.com/open-eid/cdoc2-gatling-tests 
