FROM fedora:25

# Update and get the JDK installed
RUN dnf update -y
RUN dnf install -y java-1.8.0-openjdk-devel

# Copy wildfly
COPY wildfly/wildfly /opt/jboss/wildfly

# Copy configuration over the top of it
COPY config/standalone.conf /opt/jboss/wildfly/bin/
COPY config/standalone-full.xml /opt/jboss/wildfly/standalone/configuration/
COPY config/keycloak.h2.db /opt/jboss/wildfly/standalone/data/

# Copy the apps
COPY wars/api.war /opt/jboss/wildfly/standalone/deployments/api.war
COPY wars/api.war.dodeploy /opt/jboss/wildfly/standalone/deployments/
COPY wars/rhamt-web.war /opt/jboss/wildfly/standalone/deployments/
COPY samples /samples

EXPOSE 8080

