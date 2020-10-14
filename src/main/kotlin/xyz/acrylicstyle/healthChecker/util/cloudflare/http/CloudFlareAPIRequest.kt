package xyz.acrylicstyle.healthChecker.util.cloudflare.http

import org.json.JSONObject
import util.CollectionList
import util.JSONAPI
import util.Validate
import util.reflect.Ref
import xyz.acrylicstyle.healthChecker.core.HealthCheckerConfig
import xyz.acrylicstyle.healthChecker.util.http.RequestMethod
import java.net.HttpURLConnection
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

@Suppress("LeakingThis")
open class CloudFlareAPIRequest(path: String, method: RequestMethod, body: RequestBody?): JSONAPI("https://api.cloudflare.com/client/v4/${path}", method.name) {
    constructor(path: String, method: RequestMethod): this(path, method, null)

    init {
        if (body != null) JSONAPI::class.java.getDeclaredField("requestBody").apply { isAccessible = true }.set(this, body)
        // if (body != null) println("> " + RequestBody::class.java.getDeclaredField("rawBody").apply { isAccessible = true }[body])
        Validate.notNull(HealthCheckerConfig.apiToken, "Cannot create request with null API Token")
        on("postConnection") { obj ->
            val conn = obj[0] as HttpURLConnection
            conn.doOutput = true
            conn.addRequestProperty("Content-Type", "application/json")
            conn.addRequestProperty("Authorization", "Bearer ${HealthCheckerConfig.apiToken}")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified T: Any> execute(resultClass: Class<in T>): T {
        if (Ref.getClass(resultClass).isExtends(List::class.java)) {
            val response = call()
            if (!response.response.getBoolean("success")) {
                throw RuntimeException("API Request Failed: " + response.response.getJSONArray("errors").toString(2))
            }
            val list = CollectionList<Any>()
            val clazz = typeOf<T>().arguments[0]
            val method = (clazz.type!!.classifier as KClass<*>).java.getMethod("parse", JSONObject::class.java)
            response.response.getJSONArray("result").forEach {
                list.add(method.invoke(null, it as JSONObject))
            }
            return list as T
        } else {
            val response = call()
            if (!response.response.getBoolean("success")) {
                throw RuntimeException("API Request Failed: " + response.response.getJSONArray("errors").toString(2))
            }
            return resultClass.getMethod("parse", JSONObject::class.java).invoke(null, response.response.getJSONObject("result")) as T
        }
    }
}
