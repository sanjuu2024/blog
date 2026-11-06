#!/bin/sh
set -eu

# Install with the real deployment root; Certbot runs this hook as root.
exec /opt/blog-platform/certificates/venv/bin/python \
  /opt/blog-platform/certificates/upload_oss_certificate.py \
  --config /etc/blog-platform/oss-certificate.json
