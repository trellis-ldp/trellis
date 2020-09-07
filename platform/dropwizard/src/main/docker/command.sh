#!/bin/sh
sleep 10
/opt/trellis/bin/trellis db migrate /opt/trellis/etc/config.yml
/opt/trellis/bin/trellis server /opt/trellis/etc/config.yml
