#!/bin/bash

MAVEN_HOME=/var/lib/jenkins/tools/apache-maven-3.3.9
export PATH=$PATH:$MAVEN_HOME/bin

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD_DATE=`date +%Y%m%d`

cd $DIR

rm -rf windup-web
rm -rf windup-keycloak-tool
rm -rf windup-web-distribution

git clone https://github.com/windup/windup-web.git
if [ $? != 0 ]; then
        echo "Git clone (windup-web) failed"
        exit 1
fi

cd windup-web
mvn clean install -DskipTests -Dwebpack.environment=production
if [ $? != 0 ]; then
        echo "Maven build failed for windup-web"
        exit 1
fi

cp services/target/rhamt-web/api.war ../wars/
cp ui/target/rhamt-web.war ../wars/
