# Build and run CDOC2 components

TODO: This document is not up to date. See cdoc2-java-ref-impl/test/README.md for working docker-compose example 

## Build binaries

Follow the instructions in [Main README](README.md#building) to build all Java binaries

Build Docker images locally:
```bash
./build-images.sh
```


## Testing

### Server health check

CDOC2 Key Shares server provide a health-check endpoint.
To verify that the servers are working properly, execute:

```
curl -k https://localhost:18443/actuator/health
```

### Encrypt a file using CDOC2 Key Shares Server

See `cdoc2-java-ref-impl/cdoc2-cli/README.md` for more details on how to encrypt/decrypt using Smart-ID.

