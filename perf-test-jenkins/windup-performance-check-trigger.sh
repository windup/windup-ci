#!/bin/bash

################################################################################
# Prepares Windup distribution from current state on Github to shared workspace
# and cleans shared data from old results.
#
# Expected inputs:
# * $WORKSPACE -- workspace directory, available by default in Jenkins job
# * $SHARED_WORKSPACE_DIR -- shared workspace directory, optional,
#                           /opt/perftest by default
# * $SHARED_DATA_DIR -- shared data directory, optional, /opt/data by default
# * $MAVEN_HOME_DIR -- Maven home directory, optional,
#                      /var/lib/jenkins/tools/apache-maven-3.2.5 by default
#
# Outputs:
# * New build of Windup distribution in $SHARED_WORKSPACE_DIR/windup-offline.zip
#   and unzipped to $SHARED_WORKSPACE_DIR/windup
# * New directory for summary reports with name based on current date
# * Old Windup logs are removed from current user home
# * Old results are removed from testapps_output directory in $SHARED_DATA_DIR
# * Path to Maven is set
################################################################################

SHARED_WORKSPACE=${SHARED_WORKSPACE_DIR:-/opt/perftest}
SHARED_DATA=${SHARED_DATA_DIR:-/opt/data}

### set Maven
export MAVEN_HOME=${MAVEN_HOME_DIR:-/var/lib/jenkins/tools/apache-maven-3.2.5}
export PATH=$MAVEN_HOME/bin:$PATH


### get and build Windup core
cd $WORKSPACE
rm -rf $WORKSPACE/windup
git clone https://github.com/windup/windup.git
cd $WORKSPACE/windup
mvn -DskipTests clean install


### get and build Windup Rulesets
cd $WORKSPACE
rm -rf $WORKSPACE/windup-rulesets
git clone https://github.com/windup/windup-rulesets.git
cd $WORKSPACE/windup-rulesets
mvn -DskipTests clean install


### get and build Windup Distribution
cd $WORKSPACE
rm -rf $WORKSPACE/windup-distribution
git clone https://github.com/windup/windup-distribution.git
cd $WORKSPACE/windup-distribution
mvn -DskipTests clean install


### prepare distribution into shared workspace
cd ${SHARED_WORKSPACE}
rm -rf ${SHARED_WORKSPACE}/windup
mkdir windup

cp $WORKSPACE/windup-distribution/target/*offline*.zip ${SHARED_WORKSPACE}/windup-offline.zip
unzip -q ${SHARED_WORKSPACE}/windup-offline.zip -d ${SHARED_WORKSPACE}/windup


### clean old logs and test outputs
rm -rf ~/.windup/logs/*

cd ${SHARED_DATA}
rm -rf ${SHARED_DATA}/testapps_output
mkdir testapps_output


### create new directory for summary reports
cd ${SHARED_DATA}
mkdir -p test_output_summaries/$(date +%Y_%m_%d_%H%M)
