# Build and run CDOC2 components

## Build binaries

Follow the instructions in [Main README](README.md#building) to build all Java binaries

## Docker usage

There are two docker compose files:

* docker-compose.yml - to run database scripts from source code
* docker-compose-with-pre-made-images.yml - use pre-made liquibase image for database configuration

To install the latest Docker Compose version see https://docs.docker.com/compose/install/

`.env` file contains environment variables needed to create docker images and run docker compose.

To create new shares-server image run `build-image.sh` in `shares-server` directory:
```bash
cd shares-server
./build-image.sh
```

Change to project root and to check if everything is boots up correctly run docker compose in terminal window:
```bash
docker compose -f docker-compose.yml up --build
```

When all good then exit the process and run again detached mode:  
```bash
docker kill $(docker ps -q); docker rm $(docker ps -a -q)
docker compose -f docker-compose.yml up -d
```

Application properties are loaded from `config/application.properties.docker` file.

All certificates and related are loaded from `keys` directory.

For more details on creating server certificates and trust stores, see [Generating Server keystore](keys/README.md).


### Build Docker liquibase image

Check the `.env` file for properties.

To create our pre-configured liquibase image run `create-liquibase-chanteset-image.sh` in 
`server-db` directory:
```bash
cd server-db
./create-liquibase-chanteset-image.sh
```

To use our pre-configured liquibase image run in project root:
```bash
docker compose -f docker-compose-with-pre-made-images.yml up --build
```

## Testing

### Server health check

CDOC2 Key Shares server provide a health-check endpoint.
To verify that the servers are working properly, execute:

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

