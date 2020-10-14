package xyz.acrylicstyle.healthChecker.core

import util.Collection
import util.CollectionList
import xyz.acrylicstyle.healthChecker.util.cloudflare.CFDNSRecord

object HealthCheckerConfig {
    var checkInterval = 30
    var endpoint = "https://api.cloudflare.com/client/v4/"
    var apiToken: String? = null
    var zones = CollectionList<ZoneConfig>()
    val linkedDNSRecordList = Collection<ZoneConfig, Collection<String, CFDNSRecord>>()
}
