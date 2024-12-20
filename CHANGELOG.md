# Changelog

## [0.3.0-SNAPSHOT] Release (XXXX-XX-XX)

* use [auth-token:0.2.0-SNAPSHOT](https://github.com/open-eid/cdoc2-auth) (SDJWT.body: `"aud":"https://server:port/key-shares/{shareID}?nonce={nonce}"`)
  - Fix Disclosure decoding (previously Disclosure were incorrectly decoded even when digest didn't match )
  - Use "aud" list of {server}/key-shares/{shareID}?nonce={nonce} URLs instead of custom "shareAccessData" json object.
  - remove "kid" from JWT header (duplicate of "iss" in JWT body)
  - remove "iat" and "exp" claims. Nonce creation time is checked by `cdoc2-shares-server`
  - Move x5c certificate issuer check into cdoc2-auth-token module (from `cdoc2-shares-server`) 
