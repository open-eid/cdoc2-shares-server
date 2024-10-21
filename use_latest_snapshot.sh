#!/bin/bash

#Update dependencies for latest -SNAPSHOT



# replace ee.cyber.cdoc2:* dependency versions with latest release version (includes packages from local maven repo)
mvn versions:use-latest-versions -Dincludes=ee.cyber.cdoc2:* -DexcludeReactor=false -DallowSnapshots=true

# put and get server have spring-boot as parent and need to be updated separately

mvn -f server versions:use-latest-versions -Dincludes=ee.cyber.cdoc2:* -DexcludeReactor=false -DallowSnapshots=true
