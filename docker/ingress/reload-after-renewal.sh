#!/bin/sh
set -eu

# Certbot runs deploy hooks as root; certificates stay in the shared entrypoint.
docker compose --env-file /opt/blog-platform/ingress/.env \
  -p blog-ingress -f /opt/blog-platform/ingress/compose.yaml \
  exec -T nginx nginx -t
docker compose --env-file /opt/blog-platform/ingress/.env \
  -p blog-ingress -f /opt/blog-platform/ingress/compose.yaml \
  exec -T nginx nginx -s reload
