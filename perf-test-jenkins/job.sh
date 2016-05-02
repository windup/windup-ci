export MAVEN_HOME=/var/lib/jenkins/tools/apache-maven-3.2.5/
export PATH=$MAVEN_HOME/bin:$PATH

cd $WORKSPACE
rm -rf $WORKSPACE/windup
git clone https://github.com/windup/windup.git
cd $WORKSPACE/windup
mvn -DskipTests clean install
cd $WORKSPACE

rm -rf $WORKSPACE/windup-rulesets
git clone https://github.com/windup/windup-rulesets.git
cd $WORKSPACE/windup-rulesets
mvn -DskipTests clean install
cd $WORKSPACE

rm -rf $WORKSPACE/windup-distribution
git clone https://github.com/windup/windup-distribution.git
cd $WORKSPACE/windup-distribution
mvn -DskipTests clean install
cd $WORKSPACE

cp $WORKSPACE/windup-distribution/target/*offline*.zip /opt/perftest/windup-offline.zip
cd /opt/perftest/
rm -rf ~/.windup/logs/*
/opt/perftest/Main.groovy
