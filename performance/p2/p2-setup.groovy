import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.jmeter.protocol.http.control.Header
import org.apache.jmeter.protocol.http.control.HeaderManager
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy
import java.security.MessageDigest

// P2 private configuration is UTF-8; initialization samples are outside measured results.
def configuration = new Properties()
new File(props.getProperty('configFile')).withReader('UTF-8') { configuration.load(it) }
def reserved = ['scenario', 'phase', 'threads', 'loops', 'operationDelayMs', 'scriptDir', 'configFile', 'metadataFile', 'actorSeed', 'durationSeconds', 'rampSeconds', 'throughputPerMinute']
if (configuration.keySet().any { reserved.contains(it.toString()) || it.toString().startsWith('p2.') || it.toString().startsWith('jmeter.') }) {
    throw new IllegalStateException('Private configuration must not override runtime JMeter settings')
}
configuration.each { key, value -> props.setProperty(key.toString(), value.toString()) }
def check = { boolean condition, String message -> if (!condition) throw new IllegalStateException(message) }
check(props.getProperty('testEnvironment') == 'performance', 'Isolated performance environment acknowledgement required')
def scenario = props.getProperty('scenario')
def phase = props.getProperty('phase')
def write = scenario in ['P2-BASE-07', 'P2-BASE-08', 'P2-PERF-06', 'P2-PERF-07', 'P2-PERF-08', 'P2-PERF-09', 'P2-PERF-10']
check(!write || (props.getProperty('allowWrites') == 'true' && props.getProperty('layer') == 'application'), 'Write tests require an isolated application endpoint')
['ok', 'rejected', 'unexpected'].each { props.put('p2.' + it, new java.util.concurrent.atomic.AtomicInteger()) }

// Initialization requests are deliberately excluded from measured HTTP samples.
def request = { String method, String path, Object body = null, String token = null ->
    def http = new HTTPSamplerProxy()
    http.setDomain(props.getProperty('host', '127.0.0.1'))
    http.setPort(props.getProperty('port', '18443') as int)
    http.setProtocol(props.getProperty('protocol', 'https'))
    http.setMethod(method)
    http.setPath(path)
    http.setContentEncoding('UTF-8')
    http.setConnectTimeout('10000')
    http.setResponseTimeout('30000')
    def headers = new HeaderManager()
    headers.add(new Header('Accept', 'application/json'))
    if (token) headers.add(new Header('Authorization', 'Bearer ' + token))
    if (body != null) {
        headers.add(new Header('Content-Type', 'application/json'))
        http.addNonEncodedArgument('', JsonOutput.toJson(body), '')
        http.setPostBodyRaw(true)
    }
    http.setHeaderManager(headers)
    http.threadStarted()
    try {
        def result = http.sample()
        check(result.responseCode == '200', 'Initialization HTTP request failed: ' + result.responseCode)
        def json = new JsonSlurper().parseText(result.responseDataAsString)
        check(json.code == 0, 'Initialization business code: ' + json.code)
        return [data: json.data, headers: result.responseHeaders]
    } finally {
        http.threadFinished()
    }
}
props.put('p2.request', request)
def login = { String prefix ->
    def account = props.getProperty(prefix + 'Account', '')
    def password = props.getProperty(prefix + 'Password', '')
    check((account && password) as boolean, 'Missing test credentials for ' + prefix)
    def data = request('POST', '/api/v1/auth/login', [account: account, password: password]).data
    check((data.accessToken && data.user?.id) as boolean, 'Login response lacks token or user ID')
    props.setProperty('p2.' + prefix + 'Token', data.accessToken.toString())
    props.setProperty('p2.' + prefix + 'Id', data.user.id.toString())
}
if (scenario in ['P2-PERF-03', 'P2-PERF-05', 'P2-PERF-09', 'P2-PERF-10']) login('admin')
if (scenario in ['P2-BASE-07', 'P2-PERF-04', 'P2-PERF-08', 'P2-PERF-10']) login('user')
if (scenario in ['P2-PERF-04', 'P2-PERF-10']) {
    login('otherUser')
    check(props.getProperty('p2.userId') != props.getProperty('p2.otherUserId'), 'Use distinct notification test users')
}
def requireId = { String name ->
    check(props.getProperty(name, '') ==~ /[1-9][0-9]*/, 'Missing or invalid ' + name)
}
if (scenario in ['P2-BASE-02', 'P2-BASE-04', 'P2-BASE-06', 'P2-BASE-07', 'P2-BASE-09', 'P2-PERF-07', 'P2-PERF-08', 'P2-PERF-11', 'P2-PERF-12']) requireId('articleId')
if (scenario == 'P2-PERF-02') {
    ['categoryId', 'tagId'].each { requireId(it) }
    check(props.getProperty('year', '') ==~ /[1-9][0-9]{0,3}/, 'Missing archive year')
}
if (scenario == 'P2-BASE-08') {
    check((props.getProperty('turnstileStub') == 'true' && props.getProperty('turnstileToken')) as boolean, 'Prepare a local Turnstile substitute and token first')
}
if (scenario == 'P2-PERF-08') {
    requireId('commentId')
    // Rate windows from previous scenarios must not contaminate the 31-operation test.
    Thread.sleep(61_000)
}
def metadata = [scenario: scenario, phase: phase, testMarker: '[P2-PERF] ' + props.getProperty('actorSeed')]
['articleId', 'viewArticleId', 'commentId', 'categoryId', 'tagId'].each {
    if (props.getProperty(it)) metadata[it] = props.getProperty(it)
}
def hash = { String value -> 'sha256:' + MessageDigest.getInstance('SHA-256').digest(value.getBytes('UTF-8')).encodeHex().toString() }
if (scenario == 'P2-PERF-06') {
    ['bootstrapArticleId', 'viewArticleId'].each { requireId(it) }
    check(props.getProperty('viewArticleId') != props.getProperty('bootstrapArticleId'), 'Bootstrap and measured articles must differ')
    check(props.getProperty('viewArticleId') != props.getProperty('articleId'), 'Use a dedicated measured article')
    def response = request('GET', '/api/v1/articles/' + props.getProperty('bootstrapArticleId'))
    def cookie = response.headers =~ /(?im)^set-cookie:\s*visitor_id=([^;\r\n]+)/
    check(cookie.find(), 'Bootstrap response did not provide visitor_id')
    props.setProperty('p2.sharedVisitor', cookie.group(1))
    metadata.visitorHash = hash(cookie.group(1))
}
if (scenario == 'P2-PERF-07') {
    def visitor = UUID.nameUUIDFromBytes((props.getProperty('actorSeed') + ':single').getBytes('UTF-8')).toString()
    props.setProperty('p2.sharedVisitor', visitor)
    metadata.visitorHash = hash(visitor)
    metadata.multiVisitorHashes = (0..<10).collect { index -> hash(UUID.nameUUIDFromBytes((props.getProperty('actorSeed') + ':multi:' + index).getBytes('UTF-8')).toString()) }
}
if (scenario == 'P2-PERF-09') {
    check(props.getProperty('smtpStub') == 'true', 'Only a local SMTP substitute is allowed')
    requireId('deliveryId')
    def delivery = request('GET', '/api/v1/admin/mail-deliveries/' + props.getProperty('deliveryId'), null, props.getProperty('p2.adminToken')).data
    check(delivery.status == 'FAILED', 'A valid FAILED delivery is required')
    metadata.deliveryId = props.getProperty('deliveryId')
}
if (scenario == 'P2-PERF-10') {
    check(props.getProperty('allowAllUsers') == 'true', 'Explicit ALL_USERS acknowledgement required')
    def data = request('POST', '/api/v1/admin/notifications', [title: metadata.testMarker, content: '性能测试管理员消息', targetScope: 'ALL_USERS', status: 'DRAFT'], props.getProperty('p2.adminToken')).data
    props.setProperty('p2.notificationId', data.id.toString())
    metadata.notificationId = data.id
    if (phase == 'read-all') {
        request('PATCH', '/api/v1/admin/notifications/' + data.id + '/status?status=PUBLISHED', null, props.getProperty('p2.adminToken'))
        props.setProperty('p2.otherUnread', request('GET', '/api/v1/notifications/unread-count', null, props.getProperty('p2.otherUserToken')).data.adminMessage.toString())
    }
}
['user', 'otherUser', 'admin'].each { if (props.getProperty('p2.' + it + 'Id')) metadata[it + 'Id'] = props.getProperty('p2.' + it + 'Id') }
new File(props.getProperty('metadataFile')).setText(JsonOutput.prettyPrint(JsonOutput.toJson(metadata)), 'UTF-8')
SampleResult.setResponseCode('200')
SampleResult.setResponseMessage('SETUP_OK')
SampleResult.setSuccessful(true)
