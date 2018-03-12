#!/usr/bin/env bash

DISPLAY=:99
PIDFILE=/var/xvfb_${DISPLAY:1}.pid
echo "STARTING XVFB"
nohup /usr/bin/Xvfb :99 -ac -screen 0 1920x1080x24 &
/usr/sbin/sshd -D

