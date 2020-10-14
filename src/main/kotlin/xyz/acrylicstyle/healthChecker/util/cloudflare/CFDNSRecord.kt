@file:JvmName("CFDNSRecord")

package xyz.acrylicstyle.healthChecker.util.cloudflare

import org.json.JSONObject

data class CFDNSRecord(
    val id: String,
    val type: String,
    val name: String,
    val content: String,
    val proxiable: Boolean,
    val proxied: Boolean,
    val ttl: Int,
    val locked: Boolean,
    val zone_id: String,
    val zone_name: String,
) {
    companion object {
        @Suppress("unused") // called via reflection
        @JvmStatic
        fun parse(json: JSONObject): CFDNSRecord {
            val id = json.getString("id")
            val type = json.getString("type")
            val name = json.getString("name")
            val content = json.getString("content")
            val proxiable = json.getBoolean("proxiable")
            val proxied = json.getBoolean("proxied")
            val ttl = json.getInt("ttl")
            val locked = json.getBoolean("locked")
            val zoneId = json.getString("zone_id")
            val zoneName = json.getString("zone_name")
            return CFDNSRecord(id, type, name, content, proxiable, proxied, ttl, locked, zoneId, zoneName)
        }
    }
}
