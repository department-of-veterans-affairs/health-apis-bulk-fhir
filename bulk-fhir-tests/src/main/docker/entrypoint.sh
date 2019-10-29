#!/usr/bin/env bash

set -o pipefail

[ -z "$BASE_DIR" ] && BASE_DIR="/bulk-fhir-tests"
cd $BASE_DIR

#
# Usage Information
#
usage() {
cat <<EOF
  Commands:
    list-tests
    list-categories
    test [--include-category <category>] [--exclude-category <category>] [--trust <host>] [-Dkey=value] <name> [name] [...]
    smoke-test
    regression-test

    $1
EOF
}

#
# Shell script functionality
#

doListTests() {
  local TESTS_JAR=$(find -maxdepth 1 -name "bulk-fhir-tests-*-tests.jar")
  jar -tf $TESTS_JAR \
    | grep -E '(IT|Test)\.class' \
    | sed 's/\.class//' \
    | tr / . \
    | sort
}

doListCategories() {
  local MAIN_JAR=$(find -maxdepth 1 -name "bulk-fhir-tests-*.jar" -a -not -name "bulk-fhir-tests-*-tests.jar")
  jar -tf $MAIN_JAR \
    | grep -E 'gov/va/api/health/sentinel/categories/.*\.class|gov/va/api/health/dataquery/tests/categories/.*\.class' \
    | sed 's/\.class//' \
    | tr / . \
    | sort
}

# Defaul Tests are ALL Tests
defaultTests() {
  doListTests | grep 'IT$'
}

# Runs the test using the configured properties and vars
doTest() {
  # Running specific tests otherwise do all
  local tests="$@"
  [ -z "$tests" ] && tests=$(defaultTests)

  # Filter Included and Excluded Tests
  local filter
  [ -n "${EXCLUDE_CATEGORY:-}" ] && filter+=" --filter=org.junit.experimental.categories.ExcludeCategories=$EXCLUDE_CATEGORY"
  [ -n "${INCLUDE_CATEGORY:-}" ] && filter+=" --filter=org.junit.experimental.categories.IncludeCategories=$INCLUDE_CATEGORY"

  # Less cruft in the logs
  local noise="org.junit"
  noise+="|groovy.lang.Meta"
  noise+="|io.restassured.filter"
  noise+="|io.restassured.internal"
  noise+="|java.lang.reflect"
  noise+="|java.net"
  noise+="|org.apache.http"
  noise+="|org.codehaus.groovy"
  noise+="|sun.reflect"

  # Run It
  java -cp "$(pwd)/*" $SYSTEM_PROPERTIES org.junit.runner.JUnitCore $filter $tests \
    | grep -vE "^	at ($noise)"

  # Exit on failure otherwise let other actions run.
  [ $? != 0 ] && exit 1
}

# Goes through all required variables and checks for their existence
validateProvidedVariables() {
  # Check out required deployment variables and data query specific variables.
  for param in "SENTINEL_ENV" "K8S_LOAD_BALANCER" "BULK_FHIR_API_PATH" \
  "BULK_SMOKE_TEST_CATEGORY" "BULK_REGRESSION_TEST_CATEGORY" "BULK_TOKEN"; do
    [ -z ${!param} ] && usage "Variable $param must be specified."
  done
}

# Sets up the tests with all necessary system properties
# Provides validation for required vars as well
setupForTests() {
  validateProvidedVariables

  SYSTEM_PROPERTIES="-Dsentinel=$SENTINEL_ENV \
    -Dintegration.bulkfhir.url=$K8S_LOAD_BALANCER \
    -Dintegration.bulkfhir.api-path=$BULK_FHIR_API_PATH \
    -Dbulk-token=$BULK_TOKEN"
}

# Runs Smoke Tests
doSmokeTest() {
  setupForTests

  INCLUDE_CATEGORY="$BULK_SMOKE_TEST_CATEGORY"
  doTest
}

# Runs the Regression Test Suite
doRegressionTest() {
  setupForTests

  INCLUDE_CATEGORY="$BULK_REGRESSION_TEST_CATEGORY"
  doTest
}

#
# Start
#

ARGS=$(getopt -n $(basename ${0}) \
    -l "exclude-category:,include-category:,debug,help" \
    -o "e:i:D:h" -- "$@")
[ $? != 0 ] && usage
eval set -- "$ARGS"
while true
do
  case "$1" in
    -e|--exclude-category) EXCLUDE_CATEGORY=$2;;
    -i|--include-category) INCLUDE_CATEGORY=$2;;
    -D) SYSTEM_PROPERTIES+=" -D$2";;
    --debug) set -x;;
    -h|--help) usage "I cant even with this...";;
    --) shift;break;;
  esac
  shift;
done

[ $# == 0 ] && usage "No command specified"
COMMAND=$1
shift

case "$COMMAND" in
  lc|list-categories) doListCategories;;
  lt|list-tests) doListTests;;
  t|test) doTest "$@";;
  s|smoke-test) doSmokeTest;;
  r|regression-test) doRegressionTest;;
  *) usage "Unknown command: $COMMAND";;
esac

exit 0
