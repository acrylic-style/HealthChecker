package xyz.acrylicstyle.healthChecker.util.cloudflare.dns

import org.json.JSONObject
import xyz.acrylicstyle.healthChecker.util.cloudflare.http.CloudFlareAPIRequest
import xyz.acrylicstyle.healthChecker.util.http.RequestMethod

class CFPatchSRVDNSRecord(zone_id: String, record_id: String, ttl: Int, target: String, port: Int):
    CloudFlareAPIRequest("zones/$zone_id/dns_records/$record_id", RequestMethod.PATCH, BodyBuilder().setJSON(
        JSONObject().put("data", JSONObject().put("port", port).put("target", target)).put("ttl", ttl)
    ).build())
