FROM jboss/wildfly
COPY wars/windup-web-services.war /opt/jboss/wildfly/standalone/deployments/windup-web-services.war
COPY wars/windup-web-services.war.dodeploy /opt/jboss/wildfly/standalone/deployments/
COPY wars/windup-web.war /opt/jboss/wildfly/standalone/deployments/
COPY scripts /opt/jboss/wildfly/scripts
COPY samples /samples
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/wildfly/scripts/setup.cli


