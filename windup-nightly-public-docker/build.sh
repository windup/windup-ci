#!/bin/bash

MAVEN_HOME=/var/lib/jenkins/tools/apache-maven-3.3.9
export PATH=$PATH:$MAVEN_HOME/bin

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD_DATE=`date +%Y%m%d`

cd $DIR


cd windup-web
git checkout -- .
git clean -f -d
git pull
if [ $? != 0 ]; then
	echo "Git pull failed"
	exit 1
fi
mvn clean
if [ $? != 0 ]; then
	echo "Maven clean failed"
	exit 1
fi
mvn install -DskipTests
if [ $?	!= 0 ];	then
        echo "Maven build failed"
        exit 1
fi
cd ..
rm -rf image/keycloak*
rm -rf image/META-INF
rm -rf image/wildfly*
unzip -d image/ windup-web/tests/wildfly-dist/target/windup-web-tests-wildfly-dist-*-SNAPSHOT.jar
if [ $?	!= 0 ];	then
        echo "Wildfly unzip failed"
        exit 1
fi

mv image/wildfly-*.Final image/wildfly
if [ $?	!= 0 ];	then
        echo "Wildfly copy failed"
        exit 1
fi

./image/wildfly/bin/jboss-cli.sh --commands='embed-server -c=standalone-full.xml,/subsystem=datasources/data-source=WindupServicesDS:add(jndi-name="java:jboss/datasources/WindupServicesDS", connection-url="jdbc:h2:${jboss.server.data.dir}/h2/windup-web", driver-name="h2", max-pool-size=30, user-name=sa, password=sa)'
./image/wildfly/bin/jboss-cli.sh --commands='embed-server -c=standalone-full.xml,/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=max-post-size, value=524288000)'
./image/wildfly/bin/jboss-cli.sh --commands='embed-server -c=standalone-full.xml,/system-property=keycloak.server.url:write-attribute(name=value, value=${env.KEYCLOAK_URL:/auth})'

# Copy services
cp -R windup-web/services/target/rhamt-web/api image/wildfly/standalone/deployments/api.war
if [ $?	!= 0 ];	then
        echo "Windup Web Services Copy failed"
        exit 1
fi
touch image/wildfly/standalone/deployments/api.war.dodeploy

# Copy UI
cp -R windup-web/ui/target/rhamt-web image/wildfly/standalone/deployments/rhamt-web.war
if [ $?	!= 0 ];	then
        echo "Windup Web UI Copy failed"
        exit 1
fi
touch image/wildfly/standalone/deployments/rhamt-web.war.dodeploy

cd image
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
