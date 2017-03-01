FROM fedora:25

RUN dnf update -y
RUN dnf install -y java-1.8.0-openjdk-devel java-1.8.0-openjdk-headless

ADD wildfly /opt/wildfly/
ADD samples /samples/
ADD configuration/standalone.conf /opt/wildfly/bin/standalone.conf

RUN /opt/wildfly/bin/add-user.sh admin password1. --silent

EXPOSE 8180:8180

ENTRYPOINT /opt/wildfly/bin/standalone.sh -Djboss.socket.binding.port-offset=100 -b 0.0.0.0 -Djboss.bind.address.management=0.0.0.0 -c standalone-full.xml
