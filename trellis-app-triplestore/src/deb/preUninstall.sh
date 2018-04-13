#!/bin/sh

systemctl stop trellis
systemctl disable trellis
systemctl daemon-reload
systemctl reset-failed
