"""Check P2 reporting and load the actual plans against a local HTTP substitute."""

import argparse
import csv
import datetime as dt
import importlib.util
import ipaddress
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import unittest
from unittest import mock
import re
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET

import collect_resources as collector
import resource_report


SCRIPT_DIR = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("p2_runner", SCRIPT_DIR / "run.py")
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)
JMETER = None


class ResultTests(unittest.TestCase):
    def test_medians_preserve_a_failed_round_and_exclude_warmup(self):
        with tempfile.TemporaryDirectory() as folder:
            rows = []
            for number, p95, errors, warmup in [(0, 9999, 0, True), (1, 10, 0, False), (2, 20, 1, False), (3, 30, 0, False)]:
                rows.append({"scenario": "P2-PERF-01", "phase": "read", "run": number, "warmup": warmup,
                             "sha": "test", "samples": 100, "actualRps": 5, "targetRps": 5,
                             "unexpectedErrors": errors, "errorPct": errors, "expectedRejections": 0,
                             "meanMs": p95, "p50Ms": p95, "p95Ms": p95, "p99Ms": p95,
                             "httpResult": "HTTP_FAIL" if errors else "HTTP_PASS", "jtl": "test.jtl",
                             "cpuRssHeap": "MANUAL", "dbRedisGc": "MANUAL", "dataVerification": "MANUAL"})
            summary = runner.export_results(Path(folder), rows)[0]
            self.assertEqual(summary["p95Ms"], 20)
            self.assertEqual(summary["worstP95Ms"], 30)
            self.assertEqual(summary["unexpectedErrors"], 1)
            self.assertEqual(summary["result"], "FAIL")

    def test_expected_rejections_are_separate_from_errors_and_setup_is_excluded(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "result.jtl"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(handle, ["label", "timeStamp", "elapsed", "success", "responseMessage"])
                writer.writeheader()
                writer.writerows([
                    {"label": "SETUP login", "timeStamp": 0, "elapsed": 5000, "success": "true", "responseMessage": "OK"},
                    {"label": "TARGET write", "timeStamp": 5000, "elapsed": 10, "success": "true", "responseMessage": "EXPECTED_REJECTION"},
                ])
            metrics = runner.metrics(path, "P2-BASE-07", "concurrent", 1, False, 0, "test", 0)
            self.assertEqual(metrics["samples"], 1)
            self.assertEqual(metrics["expectedRejections"], 1)
            self.assertEqual(metrics["p99Ms"], 10)
            self.assertEqual(metrics["unexpectedErrors"], 0)
            self.assertEqual(metrics['startedAt'], '1970-01-01T00:00:05.000+00:00')
            self.assertEqual(metrics['endedAt'], '1970-01-01T00:00:05.010+00:00')

    def test_private_paths_allow_ignored_files_but_reject_tracked_descendants(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder).resolve()
            subprocess.run(['git', 'init', '-q', str(root)], check=True, capture_output=True)
            (root / '.gitignore').write_text('private/\n*.properties\n', encoding='utf-8')
            private = root / 'private'
            private.mkdir()
            tracked = private / 'tracked.txt'
            tracked.write_text('fixture', encoding='utf-8')
            with mock.patch.object(runner, 'REPO_DIR', root):
                self.assertEqual(runner.private_path(root / 'local.properties'), root / 'local.properties')
                self.assertEqual(runner.private_path(private / 'new', directory=True), private / 'new')
                self.assertEqual(runner.private_path(root.parent / 'outside'), root.parent / 'outside')
                for path in (root, root / 'public.txt'):
                    with self.assertRaises(ValueError):
                        runner.private_path(path)
                subprocess.run(['git', 'add', '-f', 'private/tracked.txt'], cwd=root, check=True, capture_output=True)
                for path in (private, tracked):
                    with self.assertRaises(ValueError):
                        runner.private_path(path, directory=path.is_dir())
                with mock.patch.object(runner.subprocess, 'run', side_effect=FileNotFoundError):
                    with self.assertRaisesRegex(ValueError, 'Git is required'):
                        runner.private_path(root / 'local.properties')

    def test_legacy_copied_reports_recover_target_times_without_setup(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            row = {key: 0 for key in runner.FIELDS}
            row.update(scenario='P2-PERF-01', phase='read', run=1, warmup=False, sha='test',
                       httpResult='HTTP_PASS', jtl='C:/old-machine/results.jtl', startedAt='', endedAt='')
            jtl = root / 'P2-PERF-01-read-1/results.jtl'
            jtl.parent.mkdir()
            jtl.write_text('label,timeStamp,elapsed\nSETUP login,0,5000\nTARGET read,5000,20\n', encoding='utf-8')
            runner.export_results(root, [row])
            restored = runner.read_results(root)[0]
            self.assertEqual(restored['startedAt'], '1970-01-01T00:00:05.000+00:00')
            self.assertEqual(restored['endedAt'], '1970-01-01T00:00:05.020+00:00')
            jtl.unlink()
            self.assertEqual(runner.read_results(root)[0]['startedAt'], '')

    def test_resource_windows_exclude_warmup_and_gaps_and_export_via_cli(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source, output = root / 'resources', root / 'reports'
            source.mkdir()
            output.mkdir()
            samples = []
            for second in range(0, 91, 5):
                for service in resource_report.SERVICES:
                    sample = self.resource_sample(second, service)
                    sample['hostCpuPct'] = 20 if 10 <= second <= 30 or 60 <= second <= 80 else 999
                    sample['restartCount'] = 1 if second >= 20 else 0
                    samples.append(sample)
            resource_report.write_csv(source / 'resources-test.csv', samples)
            (source / 'collection.json').write_text('{"intervalSeconds":5}', encoding='utf-8')
            rows = []
            for run, start, end, warmup in [(0, 0, 5, True), (1, 10, 30, False), (2, 60, 80, False)]:
                row = {key: '' for key in runner.FIELDS}
                row.update(scenario='P2-PERF-01', phase='read', run=run, warmup=warmup, sha='test',
                           startedAt=self.resource_time(start), endedAt=self.resource_time(end),
                           samples=100, actualRps=5, targetRps=5, unexpectedErrors=0, errorPct=0,
                           expectedRejections=0, meanMs=10, p50Ms=10, p95Ms=10, p99Ms=10,
                           httpResult='HTTP_PASS', dataVerification='CONTENT_AND_RESOURCES_PENDING')
                rows.append(row)
            runner.export_results(output, rows)
            command = [sys.executable, str(SCRIPT_DIR / 'run.py'), '--summarize', str(output)]
            result = subprocess.run([*command, '--resources', str(source)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            with (output / 'resource-summary.csv').open(encoding='utf-8-sig', newline='') as handle:
                summaries = list(csv.DictReader(handle))
            self.assertEqual(len(summaries), 5)
            self.assertTrue(all(row['hostCpuPct'] == '20.0' for row in summaries))
            self.assertTrue(all(row['status'] == 'SAMPLED' for row in summaries))
            self.assertTrue(all(row['restartDelta'] == '1.0' for row in summaries))
            self.assertTrue(all(row['containerMemoryMiB'] == '512.0' for row in summaries))
            self.assertTrue((output / 'resource-summary.md').exists())
            before = (output / 'summary.md').read_text(encoding='utf-8')
            self.assertIn('整机CPU 20.0%', before)
            self.assertIn('nginx 0.0%', before)
            self.assertEqual(subprocess.run(command, capture_output=True).returncode, 0)
            self.assertEqual((output / 'summary.md').read_text(encoding='utf-8'), before)

    @staticmethod
    def resource_time(second):
        return (dt.datetime(2026, 11, 5, tzinfo=dt.timezone.utc) + dt.timedelta(seconds=second)).isoformat()

    def resource_sample(self, second, service='backend'):
        return dict(time=self.resource_time(second), service=service, hostCpuPct=20, hostMemoryUsedPct=30,
                    swapUsedKiB=0, containerCpuPct='0%', containerMemory='512MiB / 2GiB', javaRssKiB=1024,
                    dbConnections=4, dbActive=0, redisUsedBytes=1048576, redisPeakBytes=2097152,
                    restartCount=0, oomKilled='false', collectionError='')

    def test_resource_gaps_failures_and_short_batches_are_not_complete(self):
        row = dict(scenario='P2-PERF-01', phase='read', run=1, warmup=False,
                   startedAt=self.resource_time(10), endedAt=self.resource_time(50))
        samples = [self.resource_sample(second) for second in range(0, 61, 5)]
        for sample in samples:
            sample.update(_time=resource_report.timestamp(sample['time']), _interval=5)
        self.assertEqual(resource_report.round_resources(row, 'backend', samples)['status'], 'SAMPLED')
        # A different UTC offset represents the same instant.
        row['startedAt'] = '2026-11-05T08:00:10+08:00'
        self.assertEqual(resource_report.round_resources(row, 'backend', samples)['status'], 'SAMPLED')
        samples[5]['collectionError'] = 'failed'
        self.assertEqual(resource_report.round_resources(row, 'backend', samples)['status'], 'PARTIAL')
        samples[5]['collectionError'] = ''
        sparse = [sample for sample in samples if sample['_time'] not in [resource_report.timestamp(self.resource_time(n)) for n in (20, 25, 30, 35)]]
        self.assertEqual(resource_report.round_resources(row, 'backend', sparse)['status'], 'PARTIAL')
        self.assertEqual(resource_report.round_resources(row, 'redis', [])['status'], 'NO_SAMPLES')
        row['endedAt'] = self.resource_time(11)
        self.assertEqual(resource_report.round_resources(row, 'backend', samples)['status'], 'INSUFFICIENT')
        row['startedAt'] = ''
        self.assertEqual(resource_report.round_resources(row, 'backend', samples)['status'], 'NO_WINDOW')

    def test_auxiliary_collectors_stop_and_close_logs_on_partial_start_failure(self):
        with tempfile.TemporaryDirectory() as folder:
            process = mock.Mock()
            process.poll.return_value = None
            with mock.patch.object(collector.subprocess, 'Popen', side_effect=[process, OSError('missing tool')]) as popen:
                with self.assertRaises(OSError):
                    collector.start_helpers(Path(folder), 5)
                process.terminate.assert_called_once()
                process.wait.assert_called_once_with(timeout=5)
                self.assertTrue(popen.call_args_list[0].kwargs['stdout'].closed)
                self.assertEqual(popen.call_args_list[0].kwargs['env']['TZ'], 'UTC')

    def test_auxiliary_collectors_are_killed_if_they_ignore_termination(self):
        with tempfile.TemporaryDirectory() as folder:
            process = mock.Mock()
            process.poll.return_value = None
            process.wait.side_effect = [subprocess.TimeoutExpired('iostat', 5), 0]
            with mock.patch.object(collector.subprocess, 'Popen', return_value=process):
                helpers = collector.start_helpers(Path(folder), 5)
            # Stop one helper; the real collector creates a distinct process for each tool.
            collector.stop_helpers(helpers[:1])
            process.kill.assert_called_once()
            self.assertTrue(helpers[0]['log'].closed)
            for helper in helpers[1:]:
                helper['log'].close()

    def test_container_sampling_batches_stats_and_retains_partial_errors(self):
        containers = {'backend': 'backend-id', 'nginx': 'nginx-id'}
        stats = '\n'.join(json.dumps({'ID': value, 'CPUPerc': '0%', 'MemUsage': '1MiB / 2GiB'}) for value in containers.values())
        def command(arguments):
            if 'stats' in arguments:
                return stats
            if 'inspect' in arguments:
                return '{"restartCount":0,"oomKilled":false}'
            raise RuntimeError('backend unavailable')
        with mock.patch.object(collector, 'command', side_effect=command) as execute:
            samples = list(collector.sample_containers(['sudo', '-n', 'docker'], containers, {'hostCpuPct': 10}))
            self.assertEqual(sum('stats' in call.args[0] for call in execute.call_args_list), 1)
        self.assertTrue(samples[0]['collectionError'])
        self.assertNotIn('collectionError', samples[1])
        self.assertEqual(samples[0]['restartCount'], 0)
        self.assertIn('+00:00', samples[1]['time'])

    def test_collector_main_flushes_a_round_and_saves_metadata_when_interrupted(self):
        with tempfile.TemporaryDirectory() as folder:
            def command(arguments):
                if 'ps' in arguments:
                    return 'container-' + str(sum(ord(char) for char in ' '.join(arguments)))
                return '{"memoryBytes":1024,"nanoCpus":1000,"restartCount":0,"oomKilled":false}'
            real_exists = Path.exists
            real_read = Path.read_text
            def exists(path):
                return True if str(path).replace('\\', '/') == '/proc/meminfo' else real_exists(path)
            def read(path, *args, **kwargs):
                if str(path).replace('\\', '/') == '/proc/meminfo':
                    return 'MemTotal: 8192 kB\nMemAvailable: 4096 kB\nSwapTotal: 0 kB\nSwapFree: 0 kB\n'
                return real_read(path, *args, **kwargs)
            row = dict(time=self.resource_time(0), service='backend', hostCpuPct=10,
                       containerCpuPct='0%', containerMemory='1MiB / 2GiB', javaRssKiB=1024,
                       restartCount=0, oomKilled=False)
            with (mock.patch.object(sys, 'argv', ['collect_resources.py', '--out', folder]),
                  mock.patch.object(collector.shutil, 'which', side_effect=lambda tool: tool),
                  mock.patch.object(Path, 'exists', exists), mock.patch.object(Path, 'read_text', read),
                  mock.patch.object(collector, 'command', side_effect=command),
                  mock.patch.object(collector, 'cpu_snapshot', side_effect=[(100, 50), (200, 100)]),
                  mock.patch.object(collector, 'sample_containers', return_value=iter([row])),
                  mock.patch.object(collector, 'start_helpers', return_value=[]),
                  mock.patch.object(collector, 'stop_helpers') as stop,
                  mock.patch.object(collector.signal, 'SIGHUP', 1, create=True),
                  mock.patch.object(collector.signal, 'signal', return_value=0),
                  mock.patch.object(collector.time, 'sleep', side_effect=KeyboardInterrupt),
                  mock.patch('builtins.print')):
                collector.main()
                stop.assert_called_once_with([])
            manifest = next(Path(folder).glob('*/collection.json'))
            metadata = json.loads(manifest.read_text(encoding='utf-8'))
            self.assertIn('endedAt', metadata)
            self.assertEqual(metadata['timezone'], 'UTC')
            self.assertEqual(metadata['intervalSeconds'], 5)
            limits = json.loads(manifest.with_name('container-limits.json').read_text(encoding='utf-8'))
            self.assertEqual(set(limits), set(resource_report.SERVICES))
            with next(manifest.parent.glob('resources-*.csv')).open(encoding='utf-8-sig', newline='') as handle:
                self.assertEqual(len(list(csv.DictReader(handle))), 1)

    def test_plans_have_external_script_files_and_no_embedded_credentials(self):
        for name in ("read.jmx", "concurrency.jmx"):
            tree = ET.parse(SCRIPT_DIR / name)
            for filename in tree.findall(".//stringProp[@name='filename']"):
                self.assertTrue((SCRIPT_DIR / filename.text.split('/')[-1]).exists())
            self.assertTrue(tree.findall(".//HTTPSamplerProxy"))

    def test_p2_data_is_independent_and_matches_the_deployment_database(self):
        seed = (SCRIPT_DIR / 'data/seed.sql').read_text(encoding='utf-8')
        cleanup = (SCRIPT_DIR / 'data/cleanup.sql').read_text(encoding='utf-8')
        self.assertEqual([line for line in seed.splitlines() if line.startswith('\\ir ')], ['\\ir cleanup.sql'])
        self.assertNotIn('\\ir ', cleanup)
        self.assertIn("current_database() <> 'blog_p2_performance'", cleanup)
        self.assertIn("'p2_perf_user_'", seed)
        self.assertIn("'p2-perf-article-'", seed)
        self.assertNotIn('backend/scripts/db/performance', seed)

    def test_production_and_acceptance_resource_contracts(self):
        # Only a source contract check; docker compose config -q is still required on the host.
        production = (SCRIPT_DIR.parents[1] / 'compose.prod.yaml').read_text(encoding='utf-8')
        acceptance = (SCRIPT_DIR.parents[1] / 'compose.acceptance.yaml').read_text(encoding='utf-8')
        acceptance_nginx = (SCRIPT_DIR.parents[1] / 'docker/nginx/default.acceptance.conf').read_text(encoding='utf-8')
        production_nginx = (SCRIPT_DIR.parents[1] / 'docker/nginx/default.conf').read_text(encoding='utf-8')
        self.assertIn('mem_limit: 2560m', production)
        self.assertIn('mem_limit: 2g', production)
        self.assertIn('noeviction', production)
        self.assertNotIn('mem_limit', acceptance)
        self.assertNotIn('cpus:', acceptance)
        self.assertIn('127.0.0.1:', acceptance)
        self.assertIn('default.acceptance.conf:/etc/nginx/templates/default.conf.template:ro', acceptance)
        self.assertIn('add_header X-Robots-Tag "noindex, nofollow" always;', acceptance_nginx)
        self.assertNotIn('add_header X-Robots-Tag "noindex, nofollow" always;', production_nginx)
        self.assertIn('set_real_ip_from ${BLOG_INGRESS_IP};', acceptance_nginx)
        self.assertIn('set_real_ip_from ${BLOG_INGRESS_IP};', production_nginx)
        self.assertNotIn('listen 443', production_nginx)
        self.assertNotIn('ssl_certificate', production_nginx)
        self.assertNotIn('X-Forwarded-Proto $scheme', production_nginx)
        self.assertEqual(production_nginx.count('proxy_set_header Forwarded "";'), production_nginx.count('proxy_pass '))
        acceptance_base = '\n'.join(
            line.rstrip() for line in acceptance_nginx.splitlines()
            if line.strip() != 'add_header X-Robots-Tag "noindex, nofollow" always;'
        ) + '\n'
        self.assertEqual(acceptance_base, production_nginx.replace('\r\n', '\n'))

    def test_failed_warmup_is_reported_even_without_formal_samples(self):
        with tempfile.TemporaryDirectory() as folder:
            row = {"scenario": "P2-PERF-01", "phase": "read", "run": 0, "warmup": True,
                   "sha": "test", "samples": 0, "actualRps": 0, "targetRps": 5,
                   "unexpectedErrors": 1, "errorPct": 100, "expectedRejections": 0,
                   "meanMs": 0, "p50Ms": 0, "p95Ms": 0, "p99Ms": 0, "httpResult": "HTTP_FAIL",
                   "jtl": "test.jtl", "cpuRssHeap": "MANUAL", "dbRedisGc": "MANUAL", "dataVerification": "MANUAL"}
            summary = runner.export_results(Path(folder), [row])[0]
            self.assertEqual(summary['phase'], 'read-warmup-failed')
            self.assertEqual(summary['result'], 'FAIL')

    def test_ingress_templates_boot_without_projects_or_certificates(self):
        ingress_dir = SCRIPT_DIR.parents[1] / 'docker/ingress'
        bootstrap = (ingress_dir / 'templates/default.conf.template').read_text(encoding='utf-8')
        self.assertNotIn('ssl_certificate ', bootstrap)
        self.assertNotIn('proxy_pass ', bootstrap)
        self.assertIn('ssl_reject_handshake on;', bootstrap)
        self.assertEqual(bootstrap.count('location ^~ /.well-known/acme-challenge/'), 2)
        for filename in ('production.conf.template', 'staging.conf.template'):
            template = (ingress_dir / 'templates' / filename).read_text(encoding='utf-8')
            self.assertIn('resolver 127.0.0.11', template)
            self.assertIn('proxy_pass http://$project_upstream;', template)
            self.assertIn('client_max_body_size 12m;', template)
        staging = (ingress_dir / 'templates/staging.conf.template').read_text(encoding='utf-8')
        self.assertIn('add_header X-Robots-Tag "noindex, nofollow" always;', staging)
        production = (ingress_dir / 'templates/production.conf.template').read_text(encoding='utf-8')
        self.assertNotIn('noindex', production)

    def test_acceptance_trusts_only_its_ingress_and_preserves_nginx_variables(self):
        config = runner.read_properties(SCRIPT_DIR / 'acceptance.env.example')
        ingress_address = ipaddress.ip_address(config['BLOG_INGRESS_IP'])
        self.assertIn(ingress_address, ipaddress.ip_network('172.30.11.0/29'))
        self.assertNotIn(ingress_address, ipaddress.ip_network('172.30.10.0/29'))
        template = (SCRIPT_DIR.parents[1] / 'docker/nginx/default.acceptance.conf').read_text(encoding='utf-8')
        rendered = re.sub(r'\$\{BLOG_INGRESS_IP\}', config['BLOG_INGRESS_IP'], template)
        self.assertEqual(re.findall(r'set_real_ip_from ([^;]+);', rendered), [str(ingress_address)])
        self.assertNotIn('${', rendered)
        self.assertIn('$binary_remote_addr', rendered)
        self.assertIn('proxy_set_header Host $host;', rendered)
        self.assertIn('proxy_set_header X-Forwarded-For $remote_addr;', rendered)
        headers = (SCRIPT_DIR.parents[1] / 'docker/ingress/snippets/proxy.conf').read_text(encoding='utf-8')
        self.assertIn('proxy_set_header Forwarded "";', headers)
        self.assertIn('proxy_set_header X-Forwarded-For $remote_addr;', headers)
        self.assertNotIn('$proxy_add_x_forwarded_for', headers)


class FakeBackend(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    lock = threading.Lock()
    requests_seen = []
    comment_success = False
    admin_read = False
    published = False
    guest_success = False
    shared_likes = 0
    retried = False
    add_noindex = False

    def log_message(self, *_args):
        pass

    def handle_request(self):
        path = urlsplit(self.path).path
        body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        with self.lock:
            self.requests_seen.append((self.command, self.path, self.headers.get("Cookie")))
        status, data = 200, {}
        if path == "/api/v1/auth/login":
            account = json.loads(body)["account"]
            user_id = 2 if account.endswith("002") else 99 if account == "admin" else 1
            data = {"accessToken": "local-test-" + str(user_id), "user": {"id": user_id}}
        elif path.endswith("/unread-count"):
            own = self.headers.get("Authorization") == "Bearer local-test-1"
            data = {"total": 0 if own and self.admin_read else 21, "adminMessage": 0 if own and self.admin_read else 21}
        elif path.endswith("/read-all"):
            type(self).admin_read = True
        elif path == "/api/v1/admin/notifications" and self.command == "POST":
            type(self).published = False
            data = {"id": 123}
        elif path == "/api/v1/admin/notifications/123/status":
            with self.lock:
                if type(self).published:
                    status = 400
                type(self).published = True
        elif path.endswith("/comments") and self.command == "POST":
            with self.lock:
                if type(self).comment_success:
                    status = 429
                type(self).comment_success = True
        elif path == "/api/v1/messages" and self.command == "POST":
            with self.lock:
                if type(self).guest_success:
                    status = 429
                type(self).guest_success = True
        elif path == "/api/v1/admin/mail-deliveries/77":
            data = {"status": "FAILED"}
        elif path == "/api/v1/admin/mail-deliveries/77/retry":
            with self.lock:
                if type(self).retried:
                    status = 400
                type(self).retried = True
        elif path == "/api/v1/archives/years":
            data = [{"year": 2026, "articleCount": 25}, {"year": 2025, "articleCount": 25}]
        elif path.endswith("/like"):
            if self.headers.get("Authorization") == "Bearer local-test-1":
                with self.lock:
                    type(self).shared_likes += 1
                    if type(self).shared_likes > 30:
                        status = 429
            data = {"liked": self.command != "DELETE", "likeCount": 0 if self.command == "DELETE" else 1}
        elif path.startswith("/api/v1/articles/") and path.split('/')[-1].isdigit():
            data = {"id": int(path.split('/')[-1])}
        elif path.startswith("/api/"):
            data = {"total": 100, "records": [{"id": 1}]}
        else:
            self.send_response(200)
            html = ('<html><head><link rel="canonical" href="https://example.test/"><meta property="og:title" content="test"></head><body><div id="app"></div></body></html>')
            content = ('<rss version="2.0"></rss>' if path == '/rss.xml' else '<urlset></urlset>' if path == '/sitemap.xml' else html).encode()
            self.send_header("Content-Type", "text/html" if path not in ('/rss.xml', '/sitemap.xml') else "application/xml")
            if type(self).add_noindex:
                self.send_header("X-Robots-Tag", "noindex, nofollow")
            self.send_header("Content-Length", str(len(content)))
            self.end_headers()
            self.wfile.write(content)
            return
        rate_code = 108004 if path == '/api/v1/messages' else 103010 if path.endswith('/like') else 106004
        payload = json.dumps({"code": rate_code if status == 429 else 199001 if status == 400 else 0, "data": data}).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        if type(self).add_noindex:
            self.send_header("X-Robots-Tag", "noindex, nofollow")
        if path == "/api/v1/articles/2":
            self.send_header("Set-Cookie", "visitor_id=bootstrap-test; Path=/; HttpOnly")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    do_GET = do_POST = do_PATCH = do_DELETE = handle_request


class JMeterSmokeTests(unittest.TestCase):
    def test_real_plans_run_against_local_substitute_and_export_reports(self):
        if not JMETER:
            self.skipTest("Use --jmeter to load plans with a real JMeter executable")
        FakeBackend.requests_seen = []
        FakeBackend.comment_success = False
        FakeBackend.admin_read = False
        FakeBackend.published = False
        class LocalServer(ThreadingHTTPServer):
            request_queue_size = 128
        server = LocalServer(("127.0.0.1", 0), FakeBackend)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with tempfile.TemporaryDirectory(prefix="blog-p2-smoke-") as folder:
                root = Path(folder)
                config = root / "test.properties"
                config.write_text(
                    f"testEnvironment=performance\nhost=127.0.0.1\nport={server.server_port}\nprotocol=http\nlayer=application\n"
                    "articleId=1\nbootstrapArticleId=2\nviewArticleId=3\ncommentId=10\ncategoryId=11\ntagId=12\nyear=2026\n"
                    "userAccount=p2_perf_user_001\nuserPassword=local-test\notherUserAccount=p2_perf_user_002\notherUserPassword=local-test\n"
                    "adminAccount=admin\nadminPassword=local-test\nallowWrites=true\nallowAllUsers=true\nkeyword=性能\n",
                    encoding="utf-8")
                command = [sys.executable, str(SCRIPT_DIR / "run.py"), "--jmeter", JMETER,
                           "--properties", str(config), "--out", str(root / "reports"),
                           "--scenarios", "P2-PERF-01,P2-PERF-02,P2-PERF-04,P2-PERF-11,P2-BASE-07,P2-PERF-06,P2-PERF-07,P2-PERF-10",
                           "--warmup-seconds", "2", "--run-seconds", "3", "--formal-runs", "1", "--ramp-seconds", "1", "--html-report"]
                result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=600)
                if result.returncode:
                    # A short failure excerpt is enough to diagnose compilation, never print config or credentials.
                    errors = []
                    for path in (root / "reports").rglob("jmeter.log"):
                        errors.extend(line for line in path.read_text(encoding="utf-8", errors="replace").splitlines() if "ERROR" in line or "Exception" in line or "assert " in line)
                    for path in (root / "reports").rglob("results.jtl"):
                        with path.open(encoding="utf-8-sig", newline="") as handle:
                            errors.extend(str({key: row.get(key) for key in ['label', 'responseCode', 'responseMessage', 'failureMessage']}) for row in csv.DictReader(handle) if row.get('success') != 'true')
                    self.fail(result.stdout + result.stderr + "\n" + "\n".join(errors[:20]))
                summary = next((root / "reports").glob("*/summary.csv"))
                with summary.open(encoding="utf-8-sig", newline="") as handle:
                    rows = list(csv.DictReader(handle))
                self.assertEqual(len(rows), 11)
                self.assertTrue(all(row["result"] == "HTTP_PASS" for row in rows))
                self.assertTrue(any("%E6%80%A7%E8%83%BD" in path for _, path, _ in FakeBackend.requests_seen) or any("archives" in path for _, path, _ in FakeBackend.requests_seen))
                view_cookies = [cookie for method, path, cookie in FakeBackend.requests_seen if path == "/api/v1/articles/3"]
                self.assertEqual(len(view_cookies), 10)
                self.assertEqual(set(view_cookies), {"visitor_id=bootstrap-test"})
                self.assertTrue(next((root / "reports").rglob("report/index.html")).exists())
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)

    def test_public_reads_require_acceptance_noindex_header_when_enabled(self):
        if not JMETER:
            self.skipTest("Use --jmeter to verify the acceptance noindex response header")
        FakeBackend.add_noindex = True
        class LocalServer(ThreadingHTTPServer):
            request_queue_size = 128
        server = LocalServer(("127.0.0.1", 0), FakeBackend)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with tempfile.TemporaryDirectory(prefix="blog-p2-noindex-smoke-") as folder:
                root = Path(folder)
                config = root / "test.properties"
                config.write_text(
                    f"testEnvironment=performance\nhost=127.0.0.1\nport={server.server_port}\nprotocol=http\nlayer=nginx\nrequireNoindex=true\n"
                    "articleId=1\nadminAccount=acceptance-admin\nadminPassword=local-test\nkeyword=性能\n",
                    encoding="utf-8")
                result = subprocess.run([sys.executable, str(SCRIPT_DIR / "run.py"),
                    "--jmeter", JMETER, "--properties", str(config), "--out", str(root / "reports"),
                    "--scenarios", "P2-PERF-11", "--warmup-seconds", "2", "--run-seconds", "3",
                    "--formal-runs", "1", "--ramp-seconds", "1"],
                    capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=180)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                summary = next((root / "reports").glob("*/summary.csv"))
                with summary.open(encoding="utf-8-sig", newline="") as handle:
                    rows = list(csv.DictReader(handle))
                self.assertEqual(len(rows), 1)
                self.assertEqual(rows[0]["result"], "HTTP_PASS")
                with (summary.parent / 'run-results.csv').open(encoding='utf-8-sig', newline='') as handle:
                    rounds = list(csv.DictReader(handle))
                self.assertEqual(len(rounds), 2)
                self.assertTrue(all(resource_report.timestamp(row['endedAt']) >= resource_report.timestamp(row['startedAt']) for row in rounds))
                self.assertTrue(all(not Path(row['jtl']).is_absolute() for row in rounds))
        finally:
            FakeBackend.add_noindex = False
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)

    def test_external_scenario_branches_use_only_local_substitutes(self):
        if not JMETER:
            self.skipTest("Use --jmeter to run local-substitute scenario checks")
        FakeBackend.guest_success = False
        FakeBackend.shared_likes = 0
        FakeBackend.retried = False
        class LocalServer(ThreadingHTTPServer):
            request_queue_size = 128
        server = LocalServer(("127.0.0.1", 0), FakeBackend)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with tempfile.TemporaryDirectory(prefix='blog-p2-external-smoke-') as folder:
                root = Path(folder)
                config = root / 'test.properties'
                config.write_text(
                    f'testEnvironment=performance\nhost=127.0.0.1\nport={server.server_port}\nprotocol=http\nlayer=application\n'
                    'articleId=1\ncommentId=10\nuserAccount=p2_perf_user_001\nuserPassword=local-test\n'
                    'adminAccount=admin\nadminPassword=local-test\nallowWrites=true\nturnstileStub=true\nturnstileToken=local-test\nsmtpStub=true\ndeliveryId=77\n',
                    encoding='utf-8')
                result = subprocess.run([sys.executable, str(SCRIPT_DIR / 'run.py'),
                    '--jmeter', JMETER, '--properties', str(config), '--out', str(root / 'reports'),
                    '--scenarios', 'P2-BASE-08,P2-PERF-08,P2-PERF-09'],
                    capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=300)
                if result.returncode:
                    failures = []
                    for path in (root / 'reports').rglob('results.jtl'):
                        with path.open(encoding='utf-8-sig', newline='') as handle:
                            failures.extend(str({key: row.get(key) for key in ['label', 'responseCode', 'failureMessage']}) for row in csv.DictReader(handle) if row.get('success') != 'true')
                    self.fail(result.stdout + result.stderr + '\n' + '\n'.join(failures[:8]))
                summary = next((root / 'reports').glob('*/summary.csv'))
                with summary.open(encoding='utf-8-sig', newline='') as handle:
                    rows = list(csv.DictReader(handle))
                self.assertEqual([int(row['expectedRejections']) for row in rows], [9, 1, 9])
                self.assertEqual(FakeBackend.shared_likes, 31)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("--jmeter")
    options, rest = parser.parse_known_args()
    JMETER = options.jmeter
    unittest.main(argv=[sys.argv[0], *rest])
