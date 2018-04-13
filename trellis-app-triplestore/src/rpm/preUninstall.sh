#!/bin/sh

/usr/bin/systemctl stop trellis
/usr/bin/systemctl disable trellis
/usr/bin/systemctl daemon-reload
/usr/bin/systemctl reset-failed
