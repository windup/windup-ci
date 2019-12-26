#!/bin/bash

echo "=> Executing the commands"
$JBOSS_CLI -c << EOF
# Mark the commands below to be run as a batch
batch

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

# Execute the batch
run-batch
EOF
