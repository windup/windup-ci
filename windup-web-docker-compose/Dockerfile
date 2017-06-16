FROM jboss/keycloak-adapter-wildfly:3.1.0.Final
ENV CONFIG_FILE standalone-full.xml

# Standalone.xml modifications.
RUN sed -i -e 's/<extensions>/&\n        <extension module="org.keycloak.keycloak-adapter-subsystem"\/>/' $JBOSS_HOME/standalone/configuration/$CONFIG_FILE && \
    sed -i -e 's/<profile>/&\n        <subsystem xmlns="urn:jboss:domain:keycloak:1.1"\/>/' $JBOSS_HOME/standalone/configuration/$CONFIG_FILE && \
    sed -i -e 's/<security-domains>/&\n                <security-domain name="keycloak">\n                    <authentication>\n                        <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"\/>\n                    <\/authentication>\n                <\/security-domain>/' $JBOSS_HOME/standalone/configuration/$CONFIG_FILE

### END OF keycloak customization part

## Copy application - ANOTHER LAYER
# Copy the war files
COPY wars/api.war /opt/jboss/wildfly/standalone/deployments/api.war
COPY wars/rhamt-web.war /opt/jboss/wildfly/standalone/deployments/

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "--server-config=standalone-full.xml"]

# CUSTOMIZED SECTION - COULD BE SEPARATE DOCKER FILE ON TOP OF PREVIOUS ONE
ARG DB_HOST
ARG DB_USER
ARG DB_PASSWORD
ARG DB_URI
ARG DATA_DIR
ARG KEYCLOAK_REALM_PUBLIC_KEY
ARG KEYCLOAK_AUTH_URL

COPY config/standalone.conf /opt/jboss/wildfly/bin/
ADD startup.sh /opt/jboss/wildfly/customization/startup.sh

ENV POSTGRESQL_DRIVER_VERSION 42.1.1
RUN /opt/jboss/wildfly/customization/startup.sh standalone standalone-full.xml

# Use line below for debugging purposes
# CMD ["sh", "-c", "tail -f /dev/null"]
