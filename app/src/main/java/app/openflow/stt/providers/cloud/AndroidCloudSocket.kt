package app.openflow.stt.providers.cloud

import app.openflow.ai.providers.cloud.CloudHttpSafe
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Real OkHttp WebSocket. Blocked URLs throw. Never logs headers. */
class AndroidCloudSocket(
    private val open: (Request, WebSocketListener) -> WebSocket = { req, listener ->
        defaultClient().newWebSocket(req, listener)
    },
    private val allowUrl: (String) -> Boolean = { CloudHttpSafe.allowUrl(it) },
) : CloudSocket {

    constructor(
        client: OkHttpClient,
        allowUrl: (String) -> Boolean = { CloudHttpSafe.allowUrl(it) },
    ) : this(
        open = { req, listener -> client.newWebSocket(req, listener) },
        allowUrl = allowUrl,
    )

    override fun connect(
        url: String,
        headers: Map<String, String>,
        onError: (String) -> Unit,
        onText: (String) -> Unit,
    ): CloudSession {
        if (!allowUrl(url)) throw IOException("blocked url")
        val builder = Request.Builder().url(url)
        for ((k, v) in headers) builder.header(k, v)
        val ws = open(
            builder.build(),
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    onText(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val code = response?.code
                    val detail = when {
                        code != null -> "cloud socket failed ($code)"
                        !t.message.isNullOrBlank() -> t.message!!
                        else -> "cloud socket failed"
                    }
                    onError(detail)
                }
            },
        )
        return object : CloudSession {
            override fun send(bytes: ByteArray) {
                if (bytes.isEmpty()) return
                ws.send(bytes.toByteString())
            }

            override fun sendText(text: String) {
                ws.send(text)
            }

            override fun close() {
                ws.close(1000, null)
            }
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CloudHttpSafe.CONNECT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(CloudHttpSafe.CONNECT_MS.toLong(), TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
