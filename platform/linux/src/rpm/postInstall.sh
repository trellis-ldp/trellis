#!/bin/sh

/usr/bin/systemctl link /opt/trellis/etc/trellis.service
/usr/bin/systemctl daemon-reload
/usr/bin/systemctl start trellis
