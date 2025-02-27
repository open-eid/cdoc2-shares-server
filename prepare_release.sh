#!/bin/bash

# remove -SNAPSHOT version
# update dependencies version for ee.cyber.cdoc2.* packages with non-SNAPSHOT version
# (if local maven repo includes newer modules from cdoc2-java-ref-impl) then those are also updated
# build, test, install (local maven repo)



GIT_BRANCH=$(git branch --show-current)

CHECK_FOR_CLEAN_BRANCH=true


while getopts "v:c" opt; do
  case $opt in
    c)
      echo "Not checking for clean branch (-c)"
      CHECK_FOR_CLEAN_BRANCH=false
      ;;
    v)
      echo "Changing parent pom version to: $OPTARG"
      mvn versions:set -DnewVersion="${OPTARG}" -DupdateMatchingVersions=false
      ;;
    ?)
      echo "Invalid option: -${OPTARG}."
      exit 1
      ;;
  esac
done

if [[ "$CHECK_FOR_CLEAN_BRANCH" = true ]]; then
  echo "Checking for clean git checkout. Disable with '-c'"
  if [[ "master" != "$GIT_BRANCH" ]]; then
    echo "Not on 'master' branch. You have 5 seconds to abort."
    sleep 5
  fi

  if [[ -n $(git cherry -v) ]]; then
    echo "Detected unpushed commits. Exit"
    exit 1
  fi

  if [[ -n $(git status --porcelain --untracked-files=no) ]]; then
    echo "Uncommited changes detected. Exit"
    exit 1
  fi
else
  echo "Not checking for clean branch CHECK_FOR_CLEAN_BRANCH=$CHECK_FOR_CLEAN_BRANCH"
fi


#clean up local maven repo
#mvn dependency:purge-local-repository -Dinclude=ee.cyber.cdoc2:cdoc2-auth-token,ee.cyber.cdoc2:cdoc2-css-openapi,ee.cyber.cdoc2:cdoc2-shares-server,ee.cyber.cdoc2:cdoc2-css-db -DresolutionFuzziness=artifactId

#https://github.com/amoschov/maven-dependency-plugin/blob/master/src/site/apt/examples/purging-local-repository.apt.vm
# mvn -P 'github,!gitlab.ext' dependency:purge-local-repository -DactTransitively=false -DreResolve=false -DmanualInclude=ee.cyber.cdoc2.openapi:cdoc2-key-shares-openapi -Dverbose=true
# mvn -P 'github,!gitlab.ext' dependency:purge-local-repository -DmanualInclude=ee.cyber.cdoc2.openapi:cdoc2-css-openapi

# replace module -SNAPSHOT version with release version (non-SNAPSHOT)
mvn -f cdoc2-shares-server versions:set -DremoveSnapshot
# build and install into local maven package repository
mvn -f cdoc2-shares-server install

# update version for cdoc2-openapi module
# versions are not updated for cdoc2-key-shares-openapi OpenApi specifications
# OpenApi specifications version is parsed OAS info.version
#moved to separate repo cdoc2-openapi
#mvn -f cdoc2-openapi versions:set -DremoveSnapshot
#mvn -f cdoc2-openapi install

mvn versions:set -DremoveSnapshot

mvn -f server-openapi versions:set -DremoveSnapshot
mvn -f server-db versions:set -DremoveSnapshot
mvn -f shares-server versions:set -DremoveSnapshot

# replace ee.cyber.cdoc2:* dependency versions with latest release version (includes packages from local maven repo)
mvn versions:use-latest-versions -Dincludes=ee.cyber.cdoc2:* -DexcludeReactor=false -DallowSnapshots=false -DallowDowngrade=true


# put and get server have spring-boot as parent and need to be updated separately
mvn -f server versions:set -DremoveSnapshot
mvn -f server versions:use-latest-versions -Dincludes=ee.cyber.cdoc2:* -DexcludeReactor=false -DallowSnapshots=false -DallowDowngrade=true

# verify and install all modules
mvn install