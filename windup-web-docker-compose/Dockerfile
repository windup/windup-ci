# FROM jboss/wildfly:10.1.0.Final
FROM jboss/keycloak-adapter-wildfly:3.1.0.Final

ADD customization /opt/jboss/wildfly/customization/

COPY config/standalone.conf /opt/jboss/wildfly/bin/
# COPY config/standalone-full.xml /opt/jboss/wildfly/standalone/configuration/
# COPY config/keycloak.h2.db /opt/jboss/wildfly/standalone/data/

RUN /opt/jboss/wildfly/customization/execute.sh

# Copy the apps
COPY wars/api.war /opt/jboss/wildfly/standalone/deployments/api.war
# COPY wars/api.war.dodeploy /opt/jboss/wildfly/standalone/deployments/
COPY wars/rhamt-web.war /opt/jboss/wildfly/standalone/deployments/

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "--server-config=standalone-full.xml"]
