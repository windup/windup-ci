FROM jboss/wildfly:10.1.0.Final
# FROM jboss/keycloak-adapter-wildfly:3.1.0.Final

ENV KEYCLOAK_VERSION 3.1.0.Final

ARG DB_HOST
ARG DB_USER
ARG DB_PASSWORD
ARG DB_URI
ARG DATA_DIR
ARG KEYCLOAK_REALM_PUBLIC_KEY
ARG KEYCLOAK_AUTH_URL

WORKDIR /opt/jboss/wildfly

RUN curl -L https://downloads.jboss.org/keycloak/$KEYCLOAK_VERSION/adapters/keycloak-oidc/keycloak-wildfly-adapter-dist-$KEYCLOAK_VERSION.tar.gz | tar zx

WORKDIR /opt/jboss

# Standalone.xml modifications.
RUN sed -i -e 's/<extensions>/&\n        <extension module="org.keycloak.keycloak-adapter-subsystem"\/>/' $JBOSS_HOME/standalone/configuration/standalone-full.xml && \
    sed -i -e 's/<profile>/&\n        <subsystem xmlns="urn:jboss:domain:keycloak:1.1"\/>/' $JBOSS_HOME/standalone/configuration/standalone-full.xml && \
    sed -i -e 's/<security-domains>/&\n                <security-domain name="keycloak">\n                    <authentication>\n                        <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"\/>\n                    <\/authentication>\n                <\/security-domain>/' $JBOSS_HOME/standalone/configuration/standalone-full.xml

ADD customization /opt/jboss/wildfly/customization/

COPY config/standalone.conf /opt/jboss/wildfly/bin/
# COPY config/standalone-full.xml /opt/jboss/wildfly/standalone/configuration/
# COPY config/keycloak.h2.db /opt/jboss/wildfly/standalone/data/

RUN curl -L https://jdbc.postgresql.org/download/postgresql-42.1.1.jar > /opt/jboss/wildfly/postgresql-connector.jar

RUN /opt/jboss/wildfly/customization/execute.sh

# Copy the apps
COPY wars/api.war /opt/jboss/wildfly/standalone/deployments/api.war
# COPY wars/api.war.dodeploy /opt/jboss/wildfly/standalone/deployments/
COPY wars/rhamt-web.war /opt/jboss/wildfly/standalone/deployments/

# CMD ["sh", "-c", "tail -f /dev/null"]
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "--server-config=standalone-full.xml", "--debug"]
