#!/usr/bin/env bash

set -eu

: "${RUN_MIGRATION:=false}"
: "${RUN_APP:=true}"

# shellcheck disable=SC2086
java $JAVA_OPTS -jar ./*-allinone.jar waitOnDependencies

if [ "$RUN_MIGRATION" == "true" ]; then
  # shellcheck disable=SC2086
  java $JAVA_OPTS -jar ./*-allinone.jar migrateToInitialDbState
  # shellcheck disable=SC2086
  java $JAVA_OPTS -jar ./*-allinone.jar db migrate
fi

if [ "$RUN_APP" == "true" ]; then
  # shellcheck disable=SC2086
  exec java $JAVA_OPTS -jar ./*-allinone.jar server
fi
