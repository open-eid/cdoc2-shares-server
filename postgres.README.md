## Create postgres instance inside docker

```
docker run --name cdoc2-shares-psql -p 5432:5432 -e POSTGRES_DB=cdoc2-shares -e POSTGRES_PASSWORD=secret -d postgres

docker start cdoc2-shares-psql
docker stop cdoc2-shares-psql
```
#docker rm cdoc2-psql


## Create cdoc2-shares database

Download [cdoc2-server-liquibase](https://github.com/orgs/open-eid/packages?ecosystem=container) 
image (version must match server version) that contains liquibase changeset files specific to 
server version and create a `cdoc2-shares` database. If database is running inside Docker, 
then `--link` is required, so that liquibase container can connect to it.
```bash
docker run --rm --link cdoc2-shares-psql \
  --env DB_URL=jdbc:postgresql://cdoc2-shares-psql/cdoc2-shares \
  --env DB_PASSWORD=secret \
  --env DB_USER=postgres \
  ghcr.io/open-eid/cdoc2-shares-server-liquibase:latest
```
```
####################################################
##   _     _             _ _                      ##
##  | |   (_)           (_) |                     ##
##  | |    _  __ _ _   _ _| |__   __ _ ___  ___   ##
##  | |   | |/ _` | | | | | '_ \ / _` / __|/ _ \  ##
##  | |___| | (_| | |_| | | |_) | (_| \__ \  __/  ##
##  \_____/_|\__, |\__,_|_|_.__/ \__,_|___/\___|  ##
##              | |                               ##
##              |_|                               ##
##                                                ## 
##  Get documentation at docs.liquibase.com       ##
##  Get certified courses at learn.liquibase.com  ## 
##                                                ##
####################################################
Starting Liquibase at 13:47:45 using Java 17.0.12 (version 4.29.2 #3683 built at 2024-08-29 16:45+0000)
Liquibase Version: 4.29.2
Liquibase Open Source 4.29.2 by Liquibase
Running Changeset: db/changelog/changes/001-key_material_share.sql::1::key_material_share
Running Changeset: db/changelog/changes/002-key_material_share_nonce.sql::2::key_material_share_nonce
Running Changeset: db/changelog/changes/003-expired_key_material_share_nonce_cleanup_func.sql::3::expired_key_material_share_nonce_cleanup_func

UPDATE SUMMARY
Run:                          3
Previously run:               0
Filtered out:                 0
-------------------------------
Total change sets:            3

Liquibase: Update has been successful. Rows affected: 3
Liquibase command 'update' was executed successfully.
```

or use standard liquibase command:

```
docker run --rm --link cdoc2-shares-psql \
ghcr.io/open-eid/cdoc2-shares-server-liquibase:latest \
  --url jdbc:postgresql://cdoc2-shares-psql/cdoc2-shares \
  --username=postgres \
  --password=secret \
  --defaultsFile=liquibase.properties \
update
```

Can also be used to update DB running in other host by changing `--url`, `--username` and `--password` parameters. 
Then `--link` is not required.

More info https://hub.docker.com/r/liquibase/liquibase
