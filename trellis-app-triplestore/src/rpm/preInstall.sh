#!/bin/sh

/usr/bin/getent group trellis || /usr/sbin/groupadd -r trellis
/usr/bin/getent passwd trellis || /usr/sbin/useradd -r -g trellis -d /opt/trellis -m -s /bin/false trellis
