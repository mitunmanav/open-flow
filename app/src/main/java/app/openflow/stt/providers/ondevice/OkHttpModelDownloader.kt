package app.openflow.stt.providers.ondevice

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class OkHttpModelDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .build(),
) : ModelDownloader {
    override fun download(url: String, dest: File) {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("download ${resp.code}")
            val body = resp.body ?: error("empty body")
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
    }
}
