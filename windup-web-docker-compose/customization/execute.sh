#!/bin/bash

# Usage: execute.sh [WildFly mode] [configuration file]
#
# The default mode is 'standalone' and default configuration is based on the
# mode. It can be 'standalone.xml' or 'domain.xml'.

JBOSS_HOME=/opt/jboss/wildfly
JBOSS_CLI=$JBOSS_HOME/bin/jboss-cli.sh
JBOSS_MODE=${1:-"standalone"}
JBOSS_CONFIG=${2:-"$JBOSS_MODE-full.xml"}

function wait_for_server() {
  until `$JBOSS_CLI -c "ls /deployment" &> /dev/null`; do
    sleep 1
  done
}

echo "=> Starting WildFly server"
$JBOSS_HOME/bin/$JBOSS_MODE.sh -c $JBOSS_CONFIG > /dev/null &

echo "=> Waiting for the server to boot"
wait_for_server

echo "=> Validate configuration"
echo "=> DB_URI (docker with networking): " $DB_URI
echo "=> DB_HOST: " $DB_HOST
echo "=> DB_USER: " $DB_USER
echo "=> DATA_DIR: " $DATA_DIR
echo "=> KEYCLOAK_REALM_PUBLIC_KEY: " $KEYCLOAK_REALM_PUBLIC_KEY
echo "=> KEYLOCAK_AUTH_URL: " $KEYLOAK_AUTH_URL

CONNECTION_URL=jdbc:postgresql://$DB_URI/windup

echo "=> Executing the commands"
$JBOSS_CLI -c << EOF
# Mark the commands below to be run as a batch
batch

# Add the datasource
# Install postgres
module add --name=org.postgresql --resources=/opt/jboss/wildfly/postgresql-connector.jar --dependencies=javax.api,javax.transaction.api
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver)

data-source add --name=WindupServicesDS --driver-name=postgresql --jndi-name=java:jboss/datasources/WindupServicesDS --connection-url=$CONNECTION_URL?useUnicode=true&amp;characterEncoding=UTF-8 --user-name=$DB_USER --password="$DB_PASSWORD" --use-ccm=false --max-pool-size=25 --blocking-timeout-wait-millis=5000 --enabled=true

jms-queue add --queue-address=executorQueue --entries=queues/executorQueue
jms-queue add --queue-address=statusUpdateQueue --entries=queues/statusUpdateQueue
jms-queue add --queue-address=packageDiscoveryQueue --entries=queues/packageDiscoveryQueue
jms-topic add --topic-address=executorCancellation --entries=topics/executorCancellation

# keycloak
/subsystem=keycloak/secure-deployment=api.war:add(realm=rhamt, auth-server-url=$KEYCLOAK_AUTH_URL, public-client=true, ssl-required=EXTERNAL, resource=rhamt-web)
/subsystem=keycloak/secure-deployment=rhamt-web.war:add(realm=rhamt, auth-server-url=$KEYCLOAK_AUTH_URL, public-client=true, ssl-required=EXTERNAL, resource=rhamt-web)

# other
# Properties
/system-property=windup.data.dir:add(value="$DATA_DIR/windup-web")
/system-property=keycloak.realm.public.key:add(value="$KEYCLOAK_REALM_PUBLIC_KEY")
/system-property=keycloak.server.url:add(value="$KEYCLOAK_AUTH_URL")


# Logging
/subsystem=logging/logger=org.jboss.windup:add(level=INFO, use-parent-handlers=false, handlers=[])
/subsystem=logging/logger=org.jboss.windup.web:add(level=INFO, use-parent-handlers=false, handlers=[FILE, CONSOLE])
/subsystem=logging/logger=org.jboss.windup.web.services.WindupWebProgressMonitor:add(level=INFO, use-parent-handlers=false, handlers=[])

## Reduce the Furnace loading warnings.
/subsystem=logging/logger=org.jboss.forge.furnace.container.simple.impl.SimpleServiceRegistry/:add(level=SEVERE)
## Validator complains about "ClassX declared a normal scope but does not implement javax.enterprise.inject.spi.PassivationCapable. ..."
/subsystem=logging/logger=org.jboss.weld.Validator/:add(level=ERROR)
## DEBUG Configuring component class: ...
/subsystem=logging/logger=org.jboss.as.ee/:add(level=INFO)
## MSC000004: Failure during stop of service jboss.deployment.unit."api.war".WeldStartService: org.jboss.forge.furnace.exception.ContainerException:
## Could not get services of type [interface org.jboss.windup.web.addons.websupport.WindupWebServiceFactory] from addon [org.jboss.windup.web.addons:windup-web-support,4.0.0-SNAPSHOT +STARTED]
/subsystem=logging/logger=org.jboss.msc.service.fail/:add(level=ERROR)
## HHH000431: Unable to determine H2 database version, certain features may not work
/subsystem=logging/logger=org.hibernate.dialect.H2Dialect/:add(level=ERROR)

# Other
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=max-post-size, value=943718400)


# Execute the batch
run-batch
EOF

echo "=> Shutting down WildFly"
if [ "$JBOSS_MODE" = "standalone" ]; then
  $JBOSS_CLI -c ":shutdown"
else
  $JBOSS_CLI -c "/host=*:shutdown"
fi
