"""Exercise the installed Certbot plugin against DNS API substitutes, never real DNS."""

from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest import mock

from certbot import errors
from certbot._internal.plugins import disco
from certbot_dns_aliyun_safe import Authenticator, ExactTxtClient


class DnsCleanupTests(unittest.TestCase):
    def setUp(self):
        self.client = ExactTxtClient('fixture-access-key', 'fixture-secret')
        self.client._find_domain_id = mock.Mock(return_value='example.com')
        self.client._request = mock.Mock()
        self.domain = 'img.example.com'
        self.record_name = '_acme-challenge.img.example.com'
        self.value = 'current-challenge-value'

    def record(self, record_id, name='_acme-challenge.img', kind='TXT', value=None):
        return dict(RecordId=record_id, RR=name, Type=kind, Value=self.value if value is None else value)

    @staticmethod
    def page(records, total=None):
        return {'TotalCount': len(records) if total is None else total, 'DomainRecords': {'Record': records}}

    def deleted_ids(self):
        return [call.args[1]['RecordId'] for call in self.client._request.call_args_list
                if call.args[0] == 'DeleteDomainRecord']

    def cleanup(self):
        self.client.del_txt_record(self.domain, self.record_name, self.value)

    def test_existing_manual_txt_and_partial_matches_are_preserved(self):
        records = [self.record('manual', value='old-manual-value'),
                   self.record('other-name', name='_acme-challenge.img-copy'),
                   self.record('other-type', kind='CNAME'),
                   self.record('substring-value', value=self.value + '-other'),
                   self.record('current')]
        self.client._request.side_effect = [self.page(records), {}]
        self.cleanup()
        self.assertEqual(self.deleted_ids(), ['current'])
        query = self.client._request.call_args_list[0].args[1]
        self.assertEqual(query['RRKeyWord'], '_acme-challenge.img')
        self.assertEqual(query['TypeKeyWord'], 'TXT')
        self.assertEqual(query['ValueKeyWord'], self.value)

    def test_no_exact_match_is_a_noop(self):
        self.client._request.return_value = self.page([self.record('old', value='old-value')])
        self.cleanup()
        self.assertEqual(self.deleted_ids(), [])

    def test_exact_match_on_later_page_is_found(self):
        unrelated = [self.record(str(index), value='different-value') for index in range(100)]
        self.client._request.side_effect = [self.page(unrelated, total=101),
                                            self.page([self.record('current')], total=101), {}]
        self.cleanup()
        queries = [call.args[1] for call in self.client._request.call_args_list
                   if call.args[0] == 'DescribeDomainRecords']
        self.assertEqual([query['PageNumber'] for query in queries], [1, 2])
        self.assertTrue(all(query['PageSize'] == 100 for query in queries))
        self.assertEqual(self.deleted_ids(), ['current'])

    def test_ambiguous_exact_matches_across_pages_delete_nothing(self):
        first = [self.record('first')] + [self.record(str(index), value='different-value') for index in range(99)]
        self.client._request.side_effect = [self.page(first, total=101),
                                            self.page([self.record('second')], total=101)]
        with self.assertRaisesRegex(errors.PluginError, 'multiple exact TXT matches') as captured:
            self.cleanup()
        self.assertEqual(self.deleted_ids(), [])
        self.assertNotIn(self.value, str(captured.exception))

    def test_repeated_cleanup_after_record_is_gone_is_safe(self):
        self.client._request.side_effect = [self.page([self.record('current')]), {}, self.page([])]
        self.cleanup()
        self.cleanup()
        self.assertEqual(self.deleted_ids(), ['current'])

    def test_failed_query_does_not_fall_back_to_deleting_another_record(self):
        self.client._request.side_effect = errors.PluginError('DNS lookup failed')
        with self.assertRaises(errors.PluginError):
            self.cleanup()
        self.assertEqual(self.deleted_ids(), [])

    def test_two_current_challenges_sharing_a_name_preserve_old_record(self):
        records = [self.record('manual', value='manual-value'), self.record('one', value='challenge-one'),
                   self.record('two', value='challenge-two')]
        def request(action, arguments):
            if action == 'DescribeDomainRecords':
                return self.page(list(records))
            records[:] = [record for record in records if record['RecordId'] != arguments['RecordId']]
            return {}
        self.client._request.side_effect = request
        for challenge in ('challenge-one', 'challenge-two'):
            self.client.del_txt_record(self.domain, self.record_name, challenge)
        self.assertEqual(self.deleted_ids(), ['one', 'two'])
        self.assertEqual([record['RecordId'] for record in records], ['manual'])

    def test_real_authenticator_reads_new_credential_prefix_and_uses_exact_client(self):
        with tempfile.TemporaryDirectory() as folder:
            credentials = Path(folder) / 'credentials.ini'
            credentials.write_text('dns_aliyun_safe_access_key=fixture-access-key\n'
                                   'dns_aliyun_safe_access_key_secret=fixture-secret\n', encoding='utf-8')
            credentials.chmod(0o600)
            config = SimpleNamespace(dns_aliyun_safe_credentials=str(credentials))
            authenticator = Authenticator(config, 'dns-aliyun-safe')
            authenticator._setup_credentials()
            client = authenticator._get_alidns_client()
            self.assertIsInstance(client, ExactTxtClient)
            self.assertIs(client, authenticator._get_alidns_client())
            with (mock.patch.object(client, '_find_domain_id', return_value='example.com'),
                  mock.patch.object(client, '_request', side_effect=[{'RecordId': 'new'},
                      self.page([self.record('old', value='manual-value'), self.record('new')]), {}]) as request):
                authenticator._perform(self.domain, self.record_name, self.value)
                authenticator._cleanup(self.domain, self.record_name, self.value)
            creation = request.call_args_list[0]
            self.assertEqual(creation.args[0], 'AddDomainRecord')
            self.assertEqual(creation.args[1]['Value'], self.value)
            self.assertEqual(creation.args[1]['TTL'], authenticator.ttl)
            self.assertEqual(request.call_args_list[-1].args, ('DeleteDomainRecord', {
                'DomainName': 'example.com', 'RecordId': 'new',
            }))

    def test_installed_entrypoint_is_discovered_by_certbot(self):
        plugins = disco.PluginsRegistry.find_all()
        self.assertIn('dns-aliyun-safe', plugins)
        self.assertIs(plugins['dns-aliyun-safe'].plugin_cls, Authenticator)


if __name__ == '__main__':
    unittest.main()
