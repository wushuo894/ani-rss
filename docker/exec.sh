#!/bin/bash

chown -R ${PUID}:${PGID} /usr/app

umask ${UMASK}

exec su-exec ${PUID}:${PGID} /run.sh