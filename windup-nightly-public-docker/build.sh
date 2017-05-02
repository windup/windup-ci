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

git clone https://github.com/windup/windup-keycloak-tool.git
if [ $? != 0 ]; then
        echo "Git clone (windup-web-keycloak-tool) failed"
        exit 1
fi


git clone https://github.com/windup/windup-web-distribution.git
if [ $? != 0 ]; then
        echo "Git clone (windup-web-distribution) failed"
        exit 1
fi

cd windup-web
mvn clean install -DskipTests -Dwebpack.environment=production
if [ $? != 0 ]; then
        echo "Maven build failed for windup-web"
        exit 1
fi
cd ..


cd windup-keycloak-tool
mvn clean install
if [ $? != 0 ]; then
        echo "Maven build failed for windup-web-keycloak-tool"
        exit 1
fi
cd ..


cd windup-web-distribution
mvn clean install
if [ $? != 0 ]; then
        echo "Maven build failed for windup-web-distribution"
        exit 1
fi

unzip -d target/ target/rhamt-web-distribution-*.zip
if [ $?	!= 0 ];	then
        echo "Distribution unzip failed"
        exit 1
fi

rm target/rhamt-web-distribution-*.zip
mv target/rhamt-web-distribution-* target/rhamt-web-distribution
if [ $?	!= 0 ];	then
        echo "RHAMT copy failed"
        exit 1
fi


cd target/rhamt-web-distribution
./switch_to_authentication_required.sh

docker build -t=docker.io/windup3/windup-web_nightly .
if [ $?	!= 0 ];	then
        echo "Docker build failed"
        exit 1
fi

docker push docker.io/windup3/windup-web_nightly
if [ $?	!= 0 ];	then
        echo "Docker push failed"
        exit 1
fi
