import groovy.json.JsonSlurper
// Expected business rejections are counted separately from unexpected errors.

def scenario = props.getProperty('scenario')
def expectedRejection = false
try {
    def kind = vars.get('p2.kind')
    def status = prev.responseCode
    if (props.getProperty('layer') == 'nginx' && props.getProperty('requireNoindex') == 'true') {
        assert prev.responseHeaders.toLowerCase().contains('x-robots-tag: noindex') : 'Acceptance response is missing its noindex header'
    }
    if (kind == 'json') {
        def json = new JsonSlurper().parseText(prev.responseDataAsString)
        def code = json.code
        if (scenario in ['P2-BASE-07', 'P2-BASE-08', 'P2-PERF-09'] || (scenario == 'P2-PERF-10' && props.getProperty('phase') == 'publish')) {
            def rejection = scenario == 'P2-BASE-07' ? 106004 : scenario == 'P2-BASE-08' ? 108004 : 199001
            def rejectionStatus = scenario in ['P2-BASE-07', 'P2-BASE-08'] ? '429' : '400'
            assert (status == '200' && code == 0) || (status == rejectionStatus && code == rejection) : 'Unexpected HTTP/business code: ' + status + '/' + code
            expectedRejection = code == rejection
        } else if (scenario == 'P2-PERF-08' && vars.get('p2.operation') == '30') {
            assert status == '429' && code == 103010 : 'The 31st operation must reject with article shared-limit code'
            expectedRejection = true
        } else {
            assert status == '200' && code == 0 : 'Unexpected HTTP/business code: ' + status + '/' + code
            if (scenario == 'P2-PERF-01') assert json.data instanceof List && !json.data.isEmpty() : 'Seed multiple archive years first'
            if (scenario in ['P2-BASE-01', 'P2-BASE-03', 'P2-BASE-04', 'P2-BASE-05', 'P2-PERF-02', 'P2-PERF-05'] || (scenario == 'P2-PERF-04' && !vars.get('requestPath').contains('unread-count'))) {
                assert json.data?.total > 0 : 'Performance query must match nonempty data'
                assert json.data.records instanceof List && !json.data.records.isEmpty() : 'Performance page must contain records'
            }
            if (scenario in ['P2-BASE-02', 'P2-PERF-06']) assert json.data?.id?.toString() == props.getProperty(scenario == 'P2-PERF-06' ? 'viewArticleId' : 'articleId') : 'Wrong article destination'
            if (scenario == 'P2-PERF-07') assert json.data?.liked == (props.getProperty('phase') != 'unlike') : 'Wrong final like state'
        }
    } else {
        assert status == '200' : 'Unexpected site document HTTP code: ' + status
        def body = prev.responseDataAsString
        if (kind == 'html') {
            assert body.contains('id="app"') && body.contains('rel="canonical"') && body.contains('property="og:title"') : 'Missing Vue entry or server SEO'
            if (props.getProperty('requireNoindex') != 'true') {
                assert !body.contains('content="noindex') : 'Public HTML is unexpectedly unindexed'
            }
        } else {
            assert body.contains(kind == 'rss' ? '<rss' : '<urlset') : 'Wrong XML document'
        }
    }
    props.get(expectedRejection ? 'p2.rejected' : 'p2.ok').incrementAndGet()
    if (expectedRejection) {
        prev.setSuccessful(true)
        prev.setResponseMessage('EXPECTED_REJECTION')
    }
} catch (Exception | AssertionError error) {
    props.get('p2.unexpected').incrementAndGet()
    AssertionResult.setFailure(true)
    // Never copy response bodies, headers, tokens or user content into failure messages.
    AssertionResult.setFailureMessage(error instanceof AssertionError ? error.message.split('\n')[0] : 'Invalid response format')
}
