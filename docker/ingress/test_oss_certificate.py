"""Validate certificate deployment locally; no cloud API or real credentials are used."""

import datetime as dt
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest import mock
import xml.etree.ElementTree as ET

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
import oss2
from oss2 import xml_utils

import upload_oss_certificate as deployer


class CertificateTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.config = dict(endpoint='https://oss-cn-hangzhou.aliyuncs.com', region='cn-hangzhou',
                           bucket='fixture-bucket', domain='img.example.com', certificate_name='img.example.com-0001',
                           access_key_id='fixture-key', access_key_secret='fixture-secret')
        self.config_file = self.root / 'config.json'
        self.config_file.write_text(json.dumps(self.config), encoding='utf-8')
        self.lineage = self.root / self.config['certificate_name']
        self.lineage.mkdir()

    def make_certificate(self, domain='img.example.com', remaining_days=2):
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, domain)])
        now = dt.datetime.now(dt.timezone.utc)
        certificate = (x509.CertificateBuilder().subject_name(name).issuer_name(name)
                       .public_key(key.public_key()).serial_number(x509.random_serial_number())
                       .not_valid_before(now - dt.timedelta(days=1))
                       .not_valid_after(now + dt.timedelta(days=remaining_days))
                       .add_extension(x509.SubjectAlternativeName([x509.DNSName(domain)]), critical=False)
                       .sign(key, hashes.SHA256()))
        (self.lineage / 'fullchain.pem').write_bytes(certificate.public_bytes(serialization.Encoding.PEM))
        (self.lineage / 'privkey.pem').write_bytes(key.private_bytes(
            serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))

    def test_other_lineages_skip_without_reading_keys_or_contacting_oss(self):
        with (mock.patch.object(deployer, 'CERTIFICATE_ROOT', self.root),
              mock.patch.dict(os.environ, {'RENEWED_LINEAGE': str(self.root / 'blog.example.com')}),
              mock.patch.object(deployer, 'read_certificate') as read,
              mock.patch.object(deployer, 'deploy') as upload,
              mock.patch('builtins.print')):
            self.assertEqual(deployer.main(['--config', str(self.config_file)]), 0)
            read.assert_not_called()
            upload.assert_not_called()

    def test_check_mode_never_contacts_oss(self):
        with (mock.patch.object(deployer, 'CERTIFICATE_ROOT', self.root),
              mock.patch.dict(os.environ, {'RENEWED_LINEAGE': str(self.lineage)}),
              mock.patch.object(deployer, 'read_certificate', return_value=('certificate', 'private-key')),
              mock.patch.object(deployer, 'deploy') as upload,
              mock.patch('builtins.print')):
            self.assertEqual(deployer.main(['--config', str(self.config_file), '--check']), 0)
            upload.assert_not_called()

    def test_unsafe_certificate_name_and_http_endpoints_are_rejected(self):
        for key, value in [('certificate_name', '../other'), ('endpoint', 'http://oss-cn-hangzhou.aliyuncs.com'),
                           ('endpoint', 'https://example.com'), ('domain', '*.example.com'),
                           ('access_key_id', 'CHANGE_ME_KEY')]:
            config = {**self.config, key: value}
            self.config_file.write_text(json.dumps(config), encoding='utf-8')
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                deployer.load_configuration(self.config_file)

    def test_configuration_errors_identify_the_field_without_echoing_its_value(self):
        cases = [
            ({**self.config, 'access_key_secret': 'CHANGE_ME_fixture-secret'}, 'access_key_secret'),
            ({**self.config, 'endpoint': 'http://user:fixture-secret@example.com'}, 'endpoint'),
            ({**self.config, 'domain': 'invalid-fixture-secret'}, 'domain'),
            (['fixture-secret'], 'JSON object'),
        ]
        for config, expected in cases:
            self.config_file.write_text(json.dumps(config), encoding='utf-8')
            with self.subTest(expected=expected), self.assertRaises(deployer.DeploymentError) as captured:
                deployer.load_configuration(self.config_file)
            output = deployer.format_error(captured.exception)
            self.assertIn(expected, output)
            self.assertNotIn('fixture-secret', output)
        self.config_file.write_text('{"access_key_secret":"fixture-secret",}', encoding='utf-8')
        with self.assertRaises(deployer.DeploymentError) as captured:
            deployer.load_configuration(self.config_file)
        self.assertIn('not valid JSON', deployer.format_error(captured.exception))
        self.assertNotIn('fixture-secret', deployer.format_error(captured.exception))

    def test_missing_configuration_and_pem_files_have_specific_errors(self):
        with self.assertRaisesRegex(deployer.DeploymentError, 'Configuration file not found'):
            deployer.load_configuration(self.root / 'missing.json')
        with self.assertRaisesRegex(deployer.DeploymentError, 'fullchain.pem not found'):
            deployer.read_certificate(self.lineage, self.config['domain'])
        self.make_certificate()
        (self.lineage / 'privkey.pem').unlink()
        with self.assertRaisesRegex(deployer.DeploymentError, 'privkey.pem not found'):
            deployer.read_certificate(self.lineage, self.config['domain'])

    def test_permission_errors_do_not_echo_os_exception_details(self):
        with (mock.patch.object(Path, 'read_text', side_effect=PermissionError('fixture-secret')),
              self.assertRaises(deployer.DeploymentError) as captured):
            deployer.load_configuration(self.config_file)
        output = deployer.format_error(captured.exception)
        self.assertIn('file permissions', output)
        self.assertNotIn('fixture-secret', output)

    def test_missing_openssl_and_timeout_have_actionable_errors(self):
        self.make_certificate()
        for error, expected in [
            (FileNotFoundError('fixture-secret'), 'OpenSSL executable not found'),
            (subprocess.TimeoutExpired('openssl', 15, output='fixture-secret'), 'validation timed out'),
        ]:
            with (self.subTest(expected=expected),
                  mock.patch.object(deployer.subprocess, 'run', side_effect=error),
                  self.assertRaises(deployer.DeploymentError) as captured):
                deployer.read_certificate(self.lineage, self.config['domain'])
            output = deployer.format_error(captured.exception)
            self.assertIn(expected, output)
            self.assertNotIn('fixture-secret', output)

    def test_cli_reports_placeholder_field_and_returns_failure(self):
        config = {**self.config, 'access_key_secret': 'CHANGE_ME_fixture-secret'}
        self.config_file.write_text(json.dumps(config), encoding='utf-8')
        result = subprocess.run(
            [sys.executable, '-B', str(Path(deployer.__file__)), '--config', str(self.config_file), '--check'],
            capture_output=True, text=True, check=False, timeout=15,
        )
        self.assertEqual(result.returncode, 1)
        self.assertIn('access_key_secret is required', result.stderr)
        self.assertNotIn('fixture-secret', result.stdout + result.stderr)
        self.assertNotIn('Traceback', result.stderr)

    def test_actual_sdk_serializes_fullchain_and_uses_previous_certificate_id(self):
        binding = SimpleNamespace(domain='img.example.com', certificate=SimpleNamespace(cert_id='previous-id'))
        response = SimpleNamespace(status=200, request_id='fixture-request', headers={'x-oss-request-id': 'fixture-request'})
        with (mock.patch.object(oss2.Bucket, 'list_bucket_cname', return_value=SimpleNamespace(cname=[binding])),
              mock.patch.object(oss2.Bucket, '_Bucket__do_bucket', return_value=response) as send):
            result = deployer.deploy(self.config, 'fullchain-fixture', 'private-key-fixture')
        self.assertEqual(result.request_id, 'fixture-request')
        self.assertEqual(send.call_args.args, ('POST',))
        self.assertEqual(send.call_args.kwargs['params'], {'cname': '', 'comp': 'add'})
        document = ET.fromstring(send.call_args.kwargs['data'])
        self.assertEqual(document.findtext('Cname/Domain'), self.config['domain'])
        cert = document.find('Cname/CertificateConfiguration')
        self.assertEqual(cert.findtext('Certificate'), 'fullchain-fixture')
        self.assertEqual(cert.findtext('PrivateKey'), 'private-key-fixture')
        self.assertEqual(cert.findtext('PreviousCertId'), 'previous-id')
        self.assertIsNone(cert.find('Force'))

    def test_unbound_domains_are_never_created_and_first_cert_requires_explicit_force(self):
        with (mock.patch.object(oss2.Bucket, 'list_bucket_cname', return_value=SimpleNamespace(cname=[])),
              mock.patch.object(oss2.Bucket, 'put_bucket_cname') as send):
            with self.assertRaises(ValueError):
                deployer.deploy(self.config, 'certificate', 'private-key')
            send.assert_not_called()
        binding = SimpleNamespace(domain='img.example.com', certificate=None)
        with (mock.patch.object(oss2.Bucket, 'list_bucket_cname', return_value=SimpleNamespace(cname=[binding])),
              mock.patch.object(oss2.Bucket, 'put_bucket_cname') as send):
            deployer.deploy(self.config, 'certificate', 'private-key')
        request = send.call_args.args[0]
        document = ET.fromstring(xml_utils.to_bucket_cname_configuration(request.domain, request.cert))
        self.assertEqual(document.findtext('Cname/CertificateConfiguration/Force'), 'true')

    def test_sdk_error_output_does_not_leak_payload_or_credentials(self):
        class ApiError(Exception):
            status = 403
            code = 'AccessDenied'
            request_id = 'fixture-request'
        error = ApiError('private-key-fixture fixture-secret full-response-body')
        output = deployer.format_error(error)
        self.assertIn('status=403', output)
        self.assertIn('code=AccessDenied', output)
        self.assertNotIn('private-key-fixture', output)
        self.assertNotIn('fixture-secret', output)
        self.assertNotIn('full-response-body', output)
        self.assertNotIn('fixture-secret', deployer.format_error(ValueError('fixture-secret')))

    @unittest.skipUnless(shutil.which('openssl'), 'OpenSSL is required for certificate validation')
    def test_valid_certificate_key_matching_hostname_and_expiry(self):
        self.make_certificate()
        certificate, key = deployer.read_certificate(self.lineage, self.config['domain'])
        self.assertIn('BEGIN CERTIFICATE', certificate)
        self.assertIn('BEGIN PRIVATE KEY', key)
        with self.assertRaisesRegex(deployer.DeploymentError, 'does not cover the configured domain'):
            deployer.read_certificate(self.lineage, 'different.example.com')
        self.make_certificate(remaining_days=0.5)
        with self.assertRaisesRegex(deployer.DeploymentError, 'expires in less than 24 hours'):
            deployer.read_certificate(self.lineage, self.config['domain'])

    @unittest.skipUnless(shutil.which('openssl'), 'OpenSSL is required for certificate validation')
    def test_key_mismatch_is_rejected_before_upload(self):
        self.make_certificate()
        certificate = (self.lineage / 'fullchain.pem').read_bytes()
        self.make_certificate()
        (self.lineage / 'fullchain.pem').write_bytes(certificate)
        with self.assertRaisesRegex(deployer.DeploymentError, 'Certificate and private key do not match'):
            deployer.read_certificate(self.lineage, self.config['domain'])


if __name__ == '__main__':
    unittest.main()
