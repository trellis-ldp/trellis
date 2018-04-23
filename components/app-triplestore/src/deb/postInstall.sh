#!/bin/sh

systemctl link /opt/trellis/etc/trellis.service
systemctl daemon-reload
systemctl start trellis
