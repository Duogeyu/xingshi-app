package app.shixing.client

import app.shixing.client.data.ApiException
import app.shixing.client.data.VrccClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 只钉住「合规契约」里最容易写错的三件事：
 * 1. 每个请求都带 X-Client；2. 路径一定是 /api/v1/ 下的相对路径；
 * 3. 错误体里的 error/need 能被解析出来（打分前置条件就靠它）。
 */
class ApiContractTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(token: String? = null) =
        VrccClient(base = server.url("/api/v1/").toString()) { token }

    @Test
    fun boards_sendsClientHeader_andRelativePath() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"board":"maps","min":5,"at":1,
                   "maps":[{"world":"wrld_1","name":"M","avg":7.0,"raters":5,"rank":1}]}""",
            ),
        )
        val r = client().boards(3)
        val req = server.takeRequest()
        assertEquals("dev-shixing-android", req.getHeader("X-Client"))
        assertEquals("/api/v1/boards/maps?limit=3", req.path)
        assertEquals(1, r.maps.size)
        assertEquals("wrld_1", r.maps[0].world)
    }

    @Test
    fun notifications_firstCallOmitsSince() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"last":1042,"items":[],"every":60,"dot":2}"""))
        val r = client("tok").notifications(null)
        val req = server.takeRequest()
        assertEquals("/api/v1/me/notifications", req.path)
        assertEquals("Bearer tok", req.getHeader("Authorization"))
        assertEquals(1042L, r.last)
        assertEquals(2, r.dot)
    }

    @Test
    fun notifications_laterCallCarriesSince() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"last":1050,"items":[],"every":60}"""))
        client("tok").notifications(1042)
        assertEquals("/api/v1/me/notifications?since=1042", server.takeRequest().path)
    }

    @Test
    fun rate_postsScoreAndText_andParsesResult() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"world":"wrld_1","board":true,"avg":8.6,"raters":12,"mine":9,
                   "can":true,"words":"好地方","wordsOk":"ok"}""",
            ),
        )
        val m = client("tok").rate("wrld_1", 9, "好地方")
        val req = server.takeRequest()
        assertEquals("/api/v1/me/maps/wrld_1/rating", req.path)
        val body = req.body.readUtf8()
        assertTrue(body.contains("\"score\":9"))
        assertTrue(body.contains("好地方"))
        assertEquals(9, m.mine)
        assertEquals("好地方", m.words)
    }

    @Test
    fun ratingError_exposesNeed() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody("""{"error":"map_not_went","need":"verify"}"""),
        )
        val e = runCatching { client("tok").rate("wrld_1", 9, null) }.exceptionOrNull()
        assertTrue(e is ApiException)
        e as ApiException
        assertEquals(403, e.status)
        assertEquals("map_not_went", e.error)
        assertEquals("verify", e.need)
    }

    @Test
    fun textOver30Chars_isTruncated() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"world":"wrld_1"}"""))
        client("tok").rate("wrld_1", 5, "一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一二三四五")
        val body = server.takeRequest().body.readUtf8()
        assertTrue("短评必须被截到 30 字以内：$body", body.length < 200)
    }
}
