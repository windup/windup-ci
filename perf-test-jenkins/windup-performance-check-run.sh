#!/bin/bash

################################################################################
# Runs Windup distribution from shared workspace for given test file, compares
# outputs with previous run, stores performance results into database and
# into Google spreadsheet.
#
# Expected inputs:
# * $TEST_APP_NAME -- name of the app to be processed by Windup
# * $SHARED_WORKSPACE_DIR -- shared workspace directory, optional,
#                           /opt/perftest by default,
#                           prepared by windup-performance-check-trigger.sh
# * $SHARED_DATA_DIR -- shared data directory, optional, /opt/data by default,
#                       testapps subdirectory contains expected app files
#
# Outputs:
# * New summary reports under $SHARED_DATA_DIR/test_output_summaries -- should
#   be stored for next job runs
# * New reports for each Windup run in $SHARED_DATA_DIR/testapps_output --
#   should be deleted before next job run
################################################################################

SHARED_WORKSPACE=${SHARED_WORKSPACE_DIR:-/opt/perftest}
SHARED_DATA=${SHARED_DATA_DIR:-/opt/data}
TEST_FILES=${SHARED_DATA}/testapps

WINDUP_BIN=$(ls ${SHARED_WORKSPACE}/windup/windup*/bin/windup)


### get test app file according to given test app name
declare -A TEST_APP_MAP
TEST_APP_MAP[jee-example]="${TEST_FILES}/other/jee-example-app-1.0.0.ear"
TEST_APP_MAP[hibernate-tutorial-web]="${TEST_FILES}/other/hibernate-tutorial-web-3.3.2.GA.war"
TEST_APP_MAP[badly_named_app]="${TEST_FILES}/other/badly_named_app"
TEST_APP_MAP[drgo01.ear]="${TEST_FILES}/other/drgo01.ear"
TEST_APP_MAP[travelio.ear]="${TEST_FILES}/other/travelio.ear"
TEST_APP_MAP[ClfySmartClient.ear]="${TEST_FILES}/att/ClfySmartClient.ear"
TEST_APP_MAP[ClfyAgent.ear]="${TEST_FILES}/att/ClfyAgent.ear"
TEST_APP_MAP[PLUM_22197_ACSI_PROD_05_16_2014.ear]="${TEST_FILES}/att/PLUM_22197_ACSI_PROD_05_16_2014.ear"
TEST_APP_MAP[PassCodeReset.ear]="${TEST_FILES}/att/PassCodeReset.ear"
TEST_APP_MAP[ViewIT.war]="${TEST_FILES}/att/ViewIT.war"
TEST_APP_MAP[aps.ear]="${TEST_FILES}/att/aps.ear"
TEST_APP_MAP[phoenix-1410.ear]="${TEST_FILES}/att/phoenix-1410.ear"
TEST_APP_MAP[FKD2.ear]="${TEST_FILES}/Lantik/FKD2.ear"
TEST_APP_MAP[GW03.ear]="${TEST_FILES}/Lantik/GW03.ear"
TEST_APP_MAP[drswpc53.ear]="${TEST_FILES}/Allianz/drswpc53.ear"

TEST_APP_FILE=${TEST_APP_MAP[$TEST_APP_NAME]}


### run Windup on given test app, compare with previous run, store performance results
cd ${SHARED_WORKSPACE}
${SHARED_WORKSPACE}/Main.groovy $WINDUP_BIN $SHARED_DATA $TEST_APP_NAME $TEST_APP_FILE
