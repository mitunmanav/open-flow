package app.openflow.ai.providers.cloud

fun interface CloudHttp {
    fun post(url: String, headers: Map<String, String>, json: String): String
}
