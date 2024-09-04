#!/bin/bash

chown -R ${PUID}:${PGID} /usr/app/

umask ${UMASK}

cd /usr/app/
exec su-exec ${PUID}:${PGID} ./ani-rss