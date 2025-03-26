# Changelog

## [0.4.3]  Handle empty "" GET /key-shares header parameters better. (2025-03-26)

### Improvements:
* Handle empty ("") "x-cdoc2-auth-x5c" and "x-cdoc2-auth-ticket" header parameters better (no long stacktrace in log), return HTTP 400
* Spring Boot 3.4.1 -> 3.4.3 + other dependency updates
* Upgrade dependency `ee.cyber.cdoc2.openapi:cdoc2-key-shares-openapi` `1.0.1-draft` -> `1.0.1` (no changes besides version)

### Maven package versions:
```
cdoc2-shares-server 0.4.3
cdoc2-shares-server-pom 0.2.1
cdoc2-css-db 0.1.3
cdoc2-css-openapi 0.1.2
```

## [0.4.2] Bug fix for config property initialization (2025-02-27)

### Internal

* Bug fix for config property initialization inside `KeyShareApiService` and unit tests.

### Maven package versions:
```
cdoc2-shares-server-pom 0.2.0
cdoc2-shares-server 0.4.2
cdoc2-css-db 0.1.2
cdoc2-css-openapi 0.1.1
```

## [0.4.2-SNAPSHOT] Added index on `key_material_share_nonce` table

### Improvements:
* Added index on `key_material_share_nonce` table columns to increase query performance

## [0.4.1-SNAPSHOT] Support for JWT tokens signed with Mobile-ID (ES256)

### Features:
* Use [cdoc2-auth-token:0.3.3-SNAPSHOT](https://github.com/open-eid/cdoc2-auth) that adds support 
  for ES256 (Mobile-ID)

### Improvements:
* Certificates updates + added script that downloads and creates truststore
* Spring Boot 3.3.3 -> 3.4.1 + other dependency updates
* Tag `cdoc2-shares-server-liquibase` with `latest` when built using `build-image.sh` script  

## [0.3.0-SNAPSHOT] First public release 

* use [auth-token:0.2.0-SNAPSHOT](https://github.com/open-eid/cdoc2-auth) (SDJWT.body: `"aud":"https://server:port/key-shares/{shareID}?nonce={nonce}"`)
  - Fix Disclosure decoding (previously Disclosure were incorrectly decoded even when digest didn't match )
  - Use `"aud"` list of `{server}/key-shares/{shareID}?nonce={nonce}` URLs instead of custom `shareAccessData` json object.
  - remove `"kid"` from JWT header (duplicate of "iss" in JWT body)
  - remove `"iat"` and `"exp"` claims. Nonce creation time is checked by `cdoc2-shares-server`
  - Move x5c certificate issuer check into cdoc2-auth-token module (from `cdoc2-shares-server`) 
