# Build and run CDOC2 components

## Build binaries

Follow the instructions in [Main README](../README.md#building) to build all Java binaries

## Start CDOC2 Key Shares Server using Docker Compose

To install the latest Docker Compose version see https://docs.docker.com/compose/install/

In `cdoc2-shares-server/docker`  directory run
```
docker compose up
```

This will create the database and CDOC2 Key Shares servers.
The servers use self-signed test certificates and keystores found in `cdoc2-shares-server/keys`

For more details on creating server certificates and trust stores, see [Generating Server keystore](../keys/README.md).

## Testing

### Server health check

CDOC2 Key Shares server provide a health-check endpoint.
To verify that the servers are working properly, execute:

```
curl -k https://localhost:18442/actuator/health
```

and

```
curl -k https://localhost:18443/actuator/health
```

### Encrypt a file using CDOC2 Key Shares Server

In the `cdoc2-java-ref-impl/cdoc2-cli` repo execute:

```
java -jar target/cdoc2-cli-*.jar create \
  --server=config/localhost/localhost.properties \
  -f /path/to/enrypted-file.cdoc \
  -r EST_ID_CODE \
  /path/to/input-file
```

Replace `EST_ID_CODE` with the Estonian identification code of the recipient.

### Decrypt a file using CDOC2 Key Shares Server

In the `cdoc2-java-ref-impl/cdoc2-cli` repo execute:

```
java -jar target/cdoc2-cli*.jar decrypt \
  --server=config/localhost/localhost.properties \
  -f /path/to/enrypted-file.cdoc \
  -o /path/to/derypted-file.cdoc
```

For more details on how to use `cdoc2-cli` see [CDOC2 CLI](../cdoc2-cli/README.md).

