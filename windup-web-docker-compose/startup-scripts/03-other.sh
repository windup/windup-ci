#!/bin/bash

echo "=> DATA_DIR: " $DATA_DIR

echo "=> Executing the commands"
$JBOSS_CLI -c << EOF
# Mark the commands below to be run as a batch
batch

# JMS queues
jms-queue add --queue-address=executorQueue --entries=queues/executorQueue
jms-queue add --queue-address=statusUpdateQueue --entries=queues/statusUpdateQueue
jms-queue add --queue-address=packageDiscoveryQueue --entries=queues/packageDiscoveryQueue
jms-topic add --topic-address=executorCancellation --entries=topics/executorCancellation

# Properties
/system-property=windup.data.dir:add(value="$DATA_DIR")

# Other
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=max-post-size, value=943718400)

# Execute the batch
run-batch
EOF
