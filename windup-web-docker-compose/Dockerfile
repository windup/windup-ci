FROM jboss/keycloak-adapter-wildfly:3.1.0.Final
ENV CONFIG_FILE standalone-full.xml

# Standalone.xml modifications.
RUN sed -i -e 's/<extensions>/&\n        <extension module="org.keycloak.keycloak-adapter-subsystem"\/>/' $JBOSS_HOME/standalone/configuration/$CONFIG_FILE && \
    sed -i -e 's/<profile>/&\n        <subsystem xmlns="urn:jboss:domain:keycloak:1.1"\/>/' $JBOSS_HOME/standalone/configuration/$CONFIG_FILE && \
    sed -i -e 's/<security-domains>/&\n                <security-domain name="keycloak">\n                    <authentication>\n                        <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"\/>\n                    <\/authentication>\n                <\/security-domain>/' $JBOSS_HOME/standalone/configuration/$CONFIG_FILE

### END OF keycloak customization part

COPY config/standalone.conf /opt/jboss/wildfly/bin/
COPY startup.sh /opt/jboss/wildfly/customization/startup.sh

## Copy application - ANOTHER LAYER
# Copy the war files
COPY wars/api.war /opt/jboss/wildfly/standalone/deployments/api.war
COPY wars/rhamt-web.war /opt/jboss/wildfly/standalone/deployments/

# TODO:
# Use some similar approach as MySQL example does:
# https://github.com/docker-library/mysql/blob/master/8.0/Dockerfile
#
# Basically set startup.sh script as ENTRYPOINT and do initialization logic inside
# (make sure it is executed just once)
#
# It will also solve problem with need of duplicated env variables and build ARGS
#
# And also simplify docker image and make it more reusable
#

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "--server-config=standalone-full.xml"]

# CUSTOMIZED SECTION - COULD BE SEPARATE DOCKER FILE ON TOP OF PREVIOUS ONE
ARG DB_HOST
ARG DB_USER
ARG DB_PASSWORD
ARG DB_URI
ARG DATA_DIR
ARG KEYCLOAK_REALM_PUBLIC_KEY
ARG KEYCLOAK_AUTH_URL

ENV POSTGRESQL_DRIVER_VERSION 42.1.1
RUN /opt/jboss/wildfly/customization/startup.sh standalone standalone-full.xml

# Use line below for debugging purposes
# CMD ["sh", "-c", "tail -f /dev/null"]
