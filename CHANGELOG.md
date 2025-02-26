# Changelog

## [0.4.2-SNAPSHOT] Added index on `key_material_share_nonce` table

Features:
* Added index on `key_material_share_nonce` table columns to increase query performance

## [0.4.1-SNAPSHOT] Support for JWT tokens signed with Mobile-ID (ES256)

Features:
* Use [cdoc2-auth-token:0.3.3-SNAPSHOT](https://github.com/open-eid/cdoc2-auth) that adds support 
  for ES256 (Mobile-ID)

Improvements:
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
