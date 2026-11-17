#!/usr/bin/env python3
"""Reconfigure existing Certbot lineages to validate through Cloudflare DNS."""

import argparse
import os
from pathlib import Path
import re
import stat
import subprocess
import sys


RENEWAL_ROOT = Path('/etc/letsencrypt/renewal')


def validate_inputs(names, credentials):
    """Check local inputs without displaying credentials or modifying certificate files."""
    for name in names:
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', name):
            raise ValueError('Certificate name must match the Certificate Name shown by certbot certificates')
        if not (RENEWAL_ROOT / (name + '.conf')).is_file():
            raise ValueError('Certificate renewal configuration is missing; use its existing Certificate Name')
    if not credentials.is_absolute() or not credentials.is_file():
        raise ValueError('Cloudflare credentials must be an existing absolute file path')
    if stat.S_IMODE(credentials.stat().st_mode) & 0o077:
        raise ValueError('Cloudflare credentials must not be accessible to group or other users; use chmod 600')
    values = {}
    for line in credentials.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        key, separator, value = line.partition('=')
        if separator:
            values[key.strip()] = value.strip()
    token = values.get('dns_cloudflare_api_token', '')
    if not token or token.startswith('CHANGE_ME'):
        raise ValueError('Cloudflare API token is required in the credentials file')


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cert-name', action='append', required=True,
                        help='Existing Certificate Name; repeat for multiple certificates')
    parser.add_argument('--credentials', type=Path, required=True)
    parser.add_argument('--certbot', default='/opt/blog-platform/certificates/venv/bin/certbot')
    parser.add_argument('--propagation-seconds', type=int, default=60)
    parser.add_argument('--check', action='store_true', help='Check local inputs only; no DNS or ACME requests')
    args = parser.parse_args(argv)
    if args.propagation_seconds < 10:
        raise ValueError('DNS propagation wait must be at least 10 seconds')
    validate_inputs(args.cert_name, args.credentials)
    if args.check:
        print('Cloudflare migration: local checks passed; no certificate, DNS or hook changes')
        return 0
    if hasattr(os, 'geteuid') and os.geteuid() != 0:
        raise ValueError('Run certificate reconfiguration as root')
    for name in dict.fromkeys(args.cert_name):
        # Certbot tests the new authenticator against staging before saving the renewal options.
        # Keep lineage names, domains, existing certificates and deploy hooks under Certbot control.
        result = subprocess.run([
            args.certbot, 'reconfigure', '--non-interactive', '--cert-name', name,
            '--authenticator', 'dns-cloudflare',
            '--dns-cloudflare-credentials', str(args.credentials),
            '--dns-cloudflare-propagation-seconds', str(args.propagation_seconds),
        ], check=False)
        if result.returncode:
            print('Cloudflare migration stopped: Certbot reconfiguration failed; check the preceding Certbot output',
                  file=sys.stderr)
            return result.returncode
    print('Cloudflare migration: renewal options updated; verify renew --dry-run and existing deploy hooks next')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, OSError) as error:
        # Never include raw OS errors, command arguments containing secrets or credential contents.
        message = str(error) if isinstance(error, ValueError) else 'Cannot read local inputs or start Certbot; check paths and permissions'
        print('Cloudflare migration failed: ' + message, file=sys.stderr)
        sys.exit(1)
