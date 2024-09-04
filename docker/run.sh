#!/bin/bash

chown -R ${PUID}:${PGID} /usr/app/

umask ${UMASK}

/usr/app/ani-rss