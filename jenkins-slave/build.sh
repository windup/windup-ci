#!/bin/bash
cp ~/.ssh/id_rsa.pub .
cp ~/.docker/config.json .
docker build --force-rm --tag wonka-windup/jenkins-slave .


