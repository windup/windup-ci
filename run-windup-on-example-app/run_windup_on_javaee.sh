#run this if you changed only the windup/windup and not distribution nor ruleset.
#Then it will download and checkout appropriate version of ruleset/distribution and run them on javaee example application

SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )
rm -rf ${SCRIPTPATH}/download
mkdir ${SCRIPTPATH}/download
DOWNLOAD_PATH=${SCRIPTPATH}/download
if [ -z $WINDUP_SRC_HOME ]
then
   WINDUP_SRC_HOME=../../windup
fi
if [ ! -f "$WINDUP_SRC_HOME/pom.xml" ]
then
	echo "WINDUP_SRC_HOME set as $WINDUP_SRC_HOME does not contain pom.xml."
	exit 1

fi
cd $WINDUP_SRC_HOME
WINDUP_SRC_HOME=`pwd`
echo "Using windup located at $WINDUP_SRC_HOME"
#run the current branch
mvn clean install -DskipTests
#get current version
version=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'`

#build rulesets
git clone https://github.com/windup/windup-rulesets $DOWNLOAD_PATH/rulesets
cd $DOWNLOAD_PATH/rulesets
rulesetVersion=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'`
if [ "$rulesetVersion" != "$version" ]
then
    git checkout $version
fi
mvn clean install -DskipTests

#build distribution
git clone https://github.com/windup/windup-distribution $DOWNLOAD_PATH/distribution
cd $DOWNLOAD_PATH/distribution
distributionVersion=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'`
if [ "$distributionVersion" != "$version" ]
then
    git checkout $version
fi
mvn clean install -DskipTests

#run the builded windup
cd target
unzip windup-distribution-*.zip
cd windup-distribution-*
cd bin
echo $WINDUP_SRC_HOME/test-files/jee-example-app-1.0.0.ear
./windup --input $WINDUP_SRC_HOME/test-files/jee-example-app-1.0.0.ear --output output --overwrite
cd $SCRIPTPATH