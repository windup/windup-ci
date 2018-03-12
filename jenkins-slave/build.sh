#!/bin/bash
cp /var/lib/jenkins/.ssh/id_rsa.pub .
cp /var/lib/jenkins/.docker/config.json .
docker build --force-rm --tag wonka-windup/jenkins-slave .


