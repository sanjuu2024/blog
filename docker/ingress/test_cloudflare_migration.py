"""Offline tests; never contact a DNS provider or certificate authority."""

import contextlib
import io
import os
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest import mock

import migrate_cloudflare_dns as migration


class CloudflareMigrationTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.credentials = self.root / 'cloudflare.ini'
        self.credentials.write_text('dns_cloudflare_api_token = offline-test-secret\n', encoding='utf-8')
        self.credentials.chmod(0o600)
        self.renewal = self.root / 'img.example.com-0001.conf'
        self.renewal.write_text('authenticator = dns-aliyun-safe\ndeploy_hook = existing-hook\n', encoding='utf-8')
        self.patch = mock.patch.object(migration, 'RENEWAL_ROOT', self.root)
        self.patch.start()
        self.addCleanup(self.patch.stop)
        if os.name == 'nt':
            # Windows chmod cannot model Linux group/other bits; exercise those bits separately below.
            permissions = mock.patch.object(migration.stat, 'S_IMODE', return_value=0o600)
            permissions.start()
            self.addCleanup(permissions.stop)

    def arguments(self):
        return ['--cert-name', 'img.example.com-0001', '--credentials', str(self.credentials)]

    def test_check_mode_never_runs_certbot_and_does_not_leak_the_token(self):
        output = io.StringIO()
        with mock.patch.object(migration.subprocess, 'run') as run, contextlib.redirect_stdout(output):
            self.assertEqual(migration.main(self.arguments() + ['--check']), 0)
        run.assert_not_called()
        self.assertNotIn('offline-test-secret', output.getvalue())

    def test_reconfiguration_preserves_the_existing_lineage_and_delegates_validation_to_certbot(self):
        before = self.renewal.read_bytes()
        with mock.patch.object(migration.os, 'geteuid', return_value=0, create=True), \
                mock.patch.object(migration.subprocess, 'run', return_value=SimpleNamespace(returncode=0)) as run:
            self.assertEqual(migration.main(self.arguments()), 0)
        command = run.call_args.args[0]
        self.assertEqual(command[1:5], ['reconfigure', '--non-interactive', '--cert-name', 'img.example.com-0001'])
        self.assertIn('dns-cloudflare', command)
        self.assertNotIn('offline-test-secret', command)
        self.assertNotIn('--force-renewal', command)
        self.assertEqual(self.renewal.read_bytes(), before)

    def test_failed_reconfiguration_stops_before_the_next_certificate(self):
        (self.root / 'blog.example.com.conf').write_text('fixture', encoding='utf-8')
        with mock.patch.object(migration.os, 'geteuid', return_value=0, create=True), \
                mock.patch.object(migration.subprocess, 'run', return_value=SimpleNamespace(returncode=1)) as run:
            self.assertEqual(migration.main(self.arguments() + ['--cert-name', 'blog.example.com']), 1)
        run.assert_called_once()

    def test_placeholder_credentials_and_unsafe_lineages_are_rejected_without_running_certbot(self):
        with self.assertRaises(ValueError):
            migration.main(['--cert-name', '../other', '--credentials', str(self.credentials), '--check'])
        self.credentials.write_text('dns_cloudflare_api_token = CHANGE_ME_TOKEN\n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'token is required'):
            migration.main(self.arguments() + ['--check'])

    def test_readable_credentials_are_rejected_before_running_certbot(self):
        with mock.patch.object(migration.stat, 'S_IMODE', return_value=0o644), \
                mock.patch.object(migration.subprocess, 'run') as run:
            with self.assertRaisesRegex(ValueError, 'chmod 600'):
                migration.main(self.arguments())
        run.assert_not_called()


if __name__ == '__main__':
    unittest.main()
