package xyz.acrylicstyle.healthChecker.util.cloudflare.dns

import org.json.JSONObject
import xyz.acrylicstyle.healthChecker.util.cloudflare.http.CloudFlareAPIRequest
import xyz.acrylicstyle.healthChecker.util.http.RequestMethod

class CFPatchDNSRecord(zone_id: String, record_id: String, content: String, ttl: Int):
    CloudFlareAPIRequest("zones/$zone_id/dns_records/$record_id", RequestMethod.PATCH, BodyBuilder().setJSON(
        JSONObject().put("content", content).put("ttl", ttl)
    ).build())
