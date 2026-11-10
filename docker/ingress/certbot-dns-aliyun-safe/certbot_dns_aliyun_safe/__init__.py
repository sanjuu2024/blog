"""Reuse Aliyun DNS validation while restricting cleanup to the current TXT value."""

from certbot import errors
from certbot_dns_aliyun.alidns import AliDNSClient
from certbot_dns_aliyun.dns_aliyun import Authenticator as AliyunAuthenticator


class ExactTxtClient(AliDNSClient):
    def del_txt_record(self, domain, record_name, value):
        zone = self._find_domain_id(domain)
        relative_name = record_name[:record_name.rindex('.' + zone)]
        page, page_size = 1, 100
        matching_ids = set()
        while True:
            result = self._request('DescribeDomainRecords', {
                'DomainName': zone,
                'RRKeyWord': relative_name,
                'TypeKeyWord': 'TXT',
                'ValueKeyWord': value,
                'PageNumber': page,
                'PageSize': page_size,
            })
            # DNS search filters may return partial matches; never select by record name alone.
            for record in result['DomainRecords']['Record']:
                if record['RR'] == relative_name and record['Type'] == 'TXT' and record['Value'] == value:
                    matching_ids.add(record['RecordId'])
            if page * page_size >= int(result['TotalCount']):
                break
            page += 1
        if len(matching_ids) > 1:
            raise errors.PluginError('DNS cleanup found multiple exact TXT matches; no record was deleted')
        if matching_ids:
            self._request('DeleteDomainRecord', {'DomainName': zone, 'RecordId': matching_ids.pop()})


class Authenticator(AliyunAuthenticator):
    description = 'Aliyun DNS validation with cleanup restricted to the current TXT value.'

    def _get_alidns_client(self):
        if not self._alidns_client:
            self._alidns_client = ExactTxtClient(
                self.credentials.conf('access-key'), self.credentials.conf('access-key-secret'), self.ttl,
            )
        return self._alidns_client
