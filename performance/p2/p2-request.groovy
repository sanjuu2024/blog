import groovy.json.JsonOutput
// All scenarios belong to this P2 baseline, including the basic browsing workload.
import org.apache.jmeter.protocol.http.control.Header
import org.apache.jmeter.protocol.http.control.HeaderManager

def scenario = props.getProperty('scenario')
def phase = props.getProperty('phase')
def index = (vars.get('p2.index') ?: ctx.threadNum.toString()) as int
vars.put('p2.index', (index + 1).toString())
def article = props.getProperty('articleId', '')
def path = ''
def method = 'GET'
def body = null
def token = null
def visitor = null
def kind = 'json'
def page = index % 2 + 1
def timeZones = props.getProperty('timeZones', 'Asia/Shanghai,UTC,America/Los_Angeles').split(',')
def zone = URLEncoder.encode(timeZones[index % timeZones.length].trim(), 'UTF-8')
def keyword = URLEncoder.encode(props.getProperty('keyword', '性能'), 'UTF-8')
def mixed = {
    def roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100)
    roll < 35 ? '/api/v1/articles?pageNum=1&pageSize=10' : roll < 65 ? '/api/v1/articles/' + article : roll < 80 ? '/api/v1/articles?keyword=' + keyword + '&pageNum=1&pageSize=10' : roll < 90 ? '/api/v1/articles/' + article + '/comments?pageNum=1&pageSize=10' : '/api/v1/messages?pageNum=1&pageSize=10'
}
switch (scenario) {
    case 'P2-BASE-01': path = '/api/v1/articles?pageNum=1&pageSize=10'; break
    case 'P2-BASE-02': path = '/api/v1/articles/' + article; break
    case 'P2-BASE-03': path = '/api/v1/articles?keyword=' + keyword + '&pageNum=1&pageSize=10'; break
    case 'P2-BASE-04': path = '/api/v1/articles/' + article + '/comments?pageNum=1&pageSize=10'; break
    case 'P2-BASE-05': path = '/api/v1/messages?pageNum=1&pageSize=10'; break
    case 'P2-BASE-06': case 'P2-BASE-09': case 'P2-PERF-12': path = mixed(); break
    case 'P2-PERF-01': path = '/api/v1/archives/years?timeZone=' + zone; break
    case 'P2-PERF-02':
        def filter = ['year=' + props.getProperty('year'), 'categoryId=' + props.getProperty('categoryId'), 'tagId=' + props.getProperty('tagId')][index % 3]
        path = '/api/v1/archives/articles?' + filter + '&timeZone=' + zone + '&pageNum=' + page + '&pageSize=' + (index % 2 == 0 ? 10 : 20)
        break
    case 'P2-PERF-03': path = '/api/v1/admin/dashboard'; token = props.getProperty('p2.adminToken'); break
    case 'P2-PERF-04':
        token = props.getProperty(ctx.threadNum % 2 == 0 ? 'p2.userToken' : 'p2.otherUserToken')
        path = index % 2 == 0 ? '/api/v1/notifications?pageNum=' + ((index.intdiv(2) % 2) + 1) + '&pageSize=10' : '/api/v1/notifications/unread-count'
        break
    case 'P2-PERF-05':
        token = props.getProperty('p2.adminToken')
        path = ['/api/v1/admin/mail-deliveries?pageNum=' + page + '&pageSize=10', '/api/v1/admin/mail-deliveries?status=FAILED&pageNum=1&pageSize=10', '/api/v1/admin/security-events?pageNum=' + page + '&pageSize=10', '/api/v1/admin/security-events?eventType=LOGIN&outcome=FAILURE&pageNum=1&pageSize=10'][index % 4]
        break
    case 'P2-PERF-11':
        path = ['/', '/articles/' + article, '/sitemap.xml', '/rss.xml', '/links'][index % 5]
        kind = path == '/rss.xml' ? 'rss' : path == '/sitemap.xml' ? 'sitemap' : 'html'
        break
    case 'P2-BASE-07':
        path = '/api/v1/articles/' + article + '/comments'; method = 'POST'; token = props.getProperty('p2.userToken')
        body = [content: '[P2-PERF] ' + props.getProperty('actorSeed') + '-' + ctx.threadNum]
        break
    case 'P2-BASE-08':
        path = '/api/v1/messages'; method = 'POST'
        body = [nickname: '性能游客' + ctx.threadNum, email: '', content: '[P2-PERF] ' + props.getProperty('actorSeed') + '-' + ctx.threadNum, notifyOnReply: false, turnstileToken: props.getProperty('turnstileToken')]
        break
    case 'P2-PERF-06':
        path = '/api/v1/articles/' + props.getProperty('viewArticleId'); visitor = props.getProperty('p2.sharedVisitor'); break
    case 'P2-PERF-07':
        path = '/api/v1/articles/' + article + '/like'; method = phase == 'unlike' ? 'DELETE' : 'POST'
        visitor = phase == 'multi-like' ? UUID.nameUUIDFromBytes((props.getProperty('actorSeed') + ':multi:' + ctx.threadNum).getBytes('UTF-8')).toString() : props.getProperty('p2.sharedVisitor')
        break
    case 'P2-PERF-08':
        def operation = vars.iteration - 1
        path = operation % 2 == 0 ? '/api/v1/articles/' + article + '/like' : '/api/v1/comments/' + props.getProperty('commentId') + '/like'
        method = 'POST'; token = props.getProperty('p2.userToken')
        vars.put('p2.operation', operation.toString())
        break
    case 'P2-PERF-09': path = '/api/v1/admin/mail-deliveries/' + props.getProperty('deliveryId') + '/retry'; method = 'POST'; token = props.getProperty('p2.adminToken'); break
    case 'P2-PERF-10':
        path = phase == 'publish' ? '/api/v1/admin/notifications/' + props.getProperty('p2.notificationId') + '/status?status=PUBLISHED' : '/api/v1/notifications/read-all?category=ADMIN_MESSAGE'
        method = 'PATCH'; token = props.getProperty(phase == 'publish' ? 'p2.adminToken' : 'p2.userToken')
        break
    default: throw new IllegalArgumentException('Unknown scenario')
}
if (!visitor && path == '/api/v1/articles/' + article) {
    visitor = props.getProperty('visitorMode', 'per-thread') == 'fresh' ? UUID.randomUUID().toString() : UUID.nameUUIDFromBytes((props.getProperty('actorSeed') + ':read:' + ctx.threadNum).getBytes('UTF-8')).toString()
}
def headers = new HeaderManager()
headers.add(new Header('Accept', kind == 'json' ? 'application/json' : '*/*'))
if (token) headers.add(new Header('Authorization', 'Bearer ' + token))
if (visitor) headers.add(new Header('Cookie', 'visitor_id=' + visitor))
if (body != null) headers.add(new Header('Content-Type', 'application/json'))
sampler.setHeaderManager(headers)
sampler.setPath(path)
sampler.setMethod(method)
sampler.getArguments().removeAllArguments()
sampler.setPostBodyRaw(body != null)
if (body != null) sampler.addNonEncodedArgument('', JsonOutput.toJson(body), '')
vars.put('requestPath', path)
vars.put('requestMethod', method)
vars.put('p2.kind', kind)
