#!/bin/sh
set -eu

exec java ${JAVA_OPTS:-} -jar /opt/sihsalus/app.jar "$@"
