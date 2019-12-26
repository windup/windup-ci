#!/bin/bash

echo "=> Downloading driver...."
curl -L https://jdbc.postgresql.org/download/postgresql-$POSTGRESQL_DRIVER_VERSION.jar > /opt/jboss/wildfly/postgresql-connector.jar

echo "=> Validate configuration"
echo "=> DB_URI (docker with networking): " $DB_URI
echo "=> DB_HOST: " $DB_HOST
echo "=> DB_USER: " $DB_USER

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

# Execute the batch
run-batch
EOF
