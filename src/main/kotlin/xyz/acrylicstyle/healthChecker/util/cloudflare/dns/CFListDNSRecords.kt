package xyz.acrylicstyle.healthChecker.util.cloudflare.dns

import util.CollectionList
import xyz.acrylicstyle.healthChecker.util.cloudflare.CFDNSRecord
import xyz.acrylicstyle.healthChecker.util.cloudflare.http.CloudFlareAPIRequest
import xyz.acrylicstyle.healthChecker.util.http.RequestMethod

class CFListDNSRecords(zone: String): CloudFlareAPIRequest("zones/$zone/dns_records?per_page=100", RequestMethod.GET) {
    fun execute() = execute<CollectionList<CFDNSRecord>>(CollectionList::class.java)
}
