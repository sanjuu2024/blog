#!/usr/bin/env python3
"""Deploy a Certbot certificate to an existing OSS bucket domain binding."""

import argparse
import json
import os
from pathlib import Path
import re
import ssl
import subprocess
import sys
from urllib.parse import urlsplit


CERTIFICATE_ROOT = Path('/etc/letsencrypt/live')


class DeploymentError(ValueError):
    """An operator-facing error whose message contains no configuration values or file contents."""


def read_local_file(filename, label, encoding):
    try:
        return filename.read_text(encoding=encoding)
    except FileNotFoundError as error:
        raise DeploymentError(label + ' not found; check the configured path') from error
    except PermissionError as error:
        raise DeploymentError(label + ' cannot be read; check the process and file permissions') from error
    except UnicodeError as error:
        raise DeploymentError(label + ' has an invalid encoding; expected ' + encoding) from error
    except OSError as error:
        raise DeploymentError(label + ' cannot be read; check the file and filesystem') from error


def load_configuration(filename):
    try:
        config = json.loads(read_local_file(filename, 'Configuration file', 'utf-8'))
    except json.JSONDecodeError as error:
        raise DeploymentError('Configuration file is not valid JSON; check its syntax') from error
    if not isinstance(config, dict):
        raise DeploymentError('Configuration file must contain a JSON object')
    fields = ('endpoint', 'region', 'bucket', 'domain', 'certificate_name', 'access_key_id', 'access_key_secret')
    for key in fields:
        if not isinstance(config.get(key), str) or not config[key].strip() or config[key].strip().startswith('CHANGE_ME'):
            raise DeploymentError('Configuration field ' + key + ' is required; replace its placeholder')
    try:
        endpoint = urlsplit(config['endpoint'])
    except ValueError as error:
        raise DeploymentError('Configuration field endpoint must be a regional OSS HTTPS URL') from error
    if (endpoint.scheme != 'https' or not endpoint.hostname or not endpoint.hostname.endswith('.aliyuncs.com')
            or endpoint.username or endpoint.password or endpoint.query or endpoint.fragment
            or endpoint.path not in ('', '/')):
        raise DeploymentError('Configuration field endpoint must be a regional OSS HTTPS URL')
    if not re.fullmatch(r'[a-z0-9][a-z0-9.-]*\.[a-z]{2,}', config['domain']):
        raise DeploymentError('Configuration field domain must be an exact lowercase OSS domain')
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', config['certificate_name']):
        raise DeploymentError('Configuration field certificate_name must match the Certificate Name shown by certbot certificates')
    return config


def read_certificate(lineage, domain):
    certificate_file, key_file = lineage / 'fullchain.pem', lineage / 'privkey.pem'
    certificate = read_local_file(certificate_file, 'Certificate file fullchain.pem', 'ascii')
    private_key = read_local_file(key_file, 'Private key file privkey.pem', 'ascii')
    # OpenSSL checks hostname/expiry; the SSL context also checks that the private key matches.
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    try:
        context.load_cert_chain(str(certificate_file), str(key_file), password=lambda: '')
    except ssl.SSLError as error:
        if error.reason == 'KEY_VALUES_MISMATCH':
            raise DeploymentError('Certificate and private key do not match; check certificate_name and the PEM files') from error
        raise DeploymentError('Certificate or private key is invalid; use a valid PEM chain and an unencrypted PEM key') from error
    except OSError as error:
        raise DeploymentError('Certificate or private key cannot be loaded; check the files and permissions') from error
    # x509 only executes one check operation at a time; passing both flags would skip hostname checking.
    for arguments in (['-checkhost', domain], ['-checkend', '86400']):
        try:
            process = subprocess.run(
                ['openssl', 'x509', '-in', str(certificate_file), '-noout', *arguments],
                capture_output=True, text=True, check=False, timeout=15,
            )
        except FileNotFoundError as error:
            raise DeploymentError('OpenSSL executable not found; install openssl and check PATH') from error
        except subprocess.TimeoutExpired as error:
            raise DeploymentError('OpenSSL certificate validation timed out') from error
        except OSError as error:
            raise DeploymentError('OpenSSL could not start; check installation and executable permissions') from error
        if arguments[0] == '-checkhost':
            if process.returncode or 'does match certificate' not in process.stdout:
                raise DeploymentError('Certificate does not cover the configured domain; check domain and certificate_name')
        elif process.returncode:
            raise DeploymentError('Certificate expires in less than 24 hours; renew it before deployment')
    return certificate, private_key


def deploy(config, certificate, private_key):
    import oss2
    from oss2.models import CertInfo, PutBucketCnameRequest

    auth = oss2.AuthV4(config['access_key_id'], config['access_key_secret'])
    bucket = oss2.Bucket(auth, config['endpoint'], config['bucket'], region=config['region'], connect_timeout=20)
    binding = next((item for item in bucket.list_bucket_cname().cname if item.domain == config['domain']), None)
    if binding is None:
        raise DeploymentError('Configured domain is not bound to the OSS bucket; check bucket and domain')
    previous = binding.certificate.cert_id if binding.certificate else None
    # An existing ID protects against concurrent replacement. First attachment needs explicit Force=true.
    cert = CertInfo(certificate=certificate, private_key=private_key, previous_cert_id=previous,
                    force=None if previous else 'true')
    return bucket.put_bucket_cname(PutBucketCnameRequest(config['domain'], cert))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--check', action='store_true', help='Validate local configuration/certificate without contacting OSS')
    args = parser.parse_args(argv)
    config = load_configuration(args.config)
    lineage = CERTIFICATE_ROOT / config['certificate_name']
    renewed = os.environ.get('RENEWED_LINEAGE')
    if renewed and Path(renewed).resolve() != lineage.resolve():
        print('OSS certificate hook: skipped unrelated certificate')
        return 0
    certificate, private_key = read_certificate(lineage, config['domain'])
    if args.check:
        print('OSS certificate hook: local checks passed; no OSS request sent')
        return 0
    result = deploy(config, certificate, private_key)
    print('OSS certificate hook: deployed to ' + config['domain'] + '; request_id=' + str(result.request_id))
    return 0


def format_error(error):
    # SDK exceptions can contain request/response payloads. Never print their repr or traceback.
    details = ['type=' + type(error).__name__]
    if isinstance(error, DeploymentError):
        details.append('reason=' + str(error))
    for attribute in ('status', 'code', 'request_id'):
        value = getattr(error, attribute, None)
        if value is not None and re.fullmatch(r'[A-Za-z0-9_.:-]{1,160}', str(value)):
            details.append(attribute + '=' + str(value))
    return 'OSS certificate hook failed: ' + ' '.join(details)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except Exception as error:
        print(format_error(error), file=sys.stderr)
        sys.exit(1)
