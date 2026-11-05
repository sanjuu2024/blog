// HTTP totals do not replace database before/after verification.
def scenario = props.getProperty('scenario')
def ok = props.get('p2.ok').get()
def rejected = props.get('p2.rejected').get()
def unexpected = props.get('p2.unexpected').get()
assert unexpected == 0 : 'Unexpected operations: ' + unexpected
if (scenario in ['P2-BASE-07', 'P2-BASE-08', 'P2-PERF-09'] || (scenario == 'P2-PERF-10' && props.getProperty('phase') == 'publish')) {
    assert ok == 1 && rejected == 9 : 'Expected 1 success / 9 documented rejections; got ' + ok + '/' + rejected
} else if (scenario == 'P2-PERF-08') {
    assert ok == 30 && rejected == 1 : 'Expected 30 successful operations / 1 shared-limit rejection'
} else {
    assert ok == 10 && rejected == 0 : 'Expected 10 successful operations'
}
if (scenario == 'P2-PERF-10' && props.getProperty('phase') == 'read-all') {
    def request = props.get('p2.request')
    def user = request('GET', '/api/v1/notifications/unread-count', null, props.getProperty('p2.userToken')).data
    def other = request('GET', '/api/v1/notifications/unread-count', null, props.getProperty('p2.otherUserToken')).data
    assert user.adminMessage == 0 : 'First user still has unread admin messages'
    assert other.adminMessage.toString() == props.getProperty('p2.otherUnread') : 'Read-all changed another user'
}
SampleResult.setResponseCode('200')
SampleResult.setResponseMessage('HTTP_VERIFIED_DB_PENDING')
SampleResult.setSuccessful(true)
