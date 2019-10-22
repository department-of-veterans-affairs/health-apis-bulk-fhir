#!/usr/bin/env bash

set -o pipefail

usage() {
cat <<EOF
  Commands:
    smoke-test
    regression-test
EOF
}

doSmokeTest() {
  echo "Doin the smoke tests!!!"
  exit 1
}

doRegressionTest() {
  echo "Doin the regression tests!!!"
  exit 0
}

[ $# == 0 ] && usage "No command specified"
COMMAND=$1
shift

case "$COMMAND" in
  s|smoke-test) doSmokeTest;;
  r|regression-test) doRegressionTest;;
  *) usage "Unknown command: $COMMAND";;
esac

exit 0
