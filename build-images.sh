#!/usr/bin/env bash

# build Docker images locally
PROJECT_DIR=$(pwd)

cd $PROJECT_DIR/shares-server
bash build-image.sh

cd $PROJECT_DIR/server-db
bash build-image.sh

cd $PROJECT_DIR