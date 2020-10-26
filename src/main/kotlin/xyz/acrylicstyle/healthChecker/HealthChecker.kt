package xyz.acrylicstyle.healthChecker

import org.apache.logging.log4j.LogManager
import util.CollectionList
import util.promise.Promise
import util.yaml.YamlConfiguration
import xyz.acrylicstyle.healthChecker.core.HealthCheckerConfig
import xyz.acrylicstyle.healthChecker.core.ReportedException
import xyz.acrylicstyle.healthChecker.util.cloudflare.dns.CFPatchDNSRecord
import xyz.acrylicstyle.healthChecker.util.cloudflare.dns.CFPatchSRVDNSRecord
import java.net.InetSocketAddress
import java.net.Socket

object HealthChecker: Runnable {
    private val logger = LogManager.getLogger(HealthChecker::class.java)

    private var lastTick = System.currentTimeMillis()

    val config = YamlConfiguration("./config.yml").asObject()

    private fun isReachable(host: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port))
            val conn = socket.isConnected
            socket.close()
            conn
        } catch (e: Exception) {
            false
        }
    }

    override fun run() {
        logger.info("1 tick = ${HealthCheckerConfig.checkInterval * 1000}ms")
        while (true) {
            HealthCheckerConfig.linkedDNSRecordList.forEach { zone, map ->
                map.forEach f@ { name, record ->
                    val results = CollectionList<Int>()
                    val promises = CollectionList<Promise<*>>()
                    zone.groups[name]!!.targets.foreach { target, i ->
                        promises.add(Promise.async {
                            try {
                                if (isReachable(target.ip, target.port)) {
                                    results.add(i)
                                }
                            } catch (e: IllegalArgumentException) {
                                throw ReportedException("Illegal port or IP (${target.ip}:${target.port})", e)
                            }
                        })
                    }
                    Promise.all(*promises.toTypedArray()).complete()
                    results.sort()
                    if (results.isNotEmpty()) {
                        val target = zone.groups[name]!!.targets[results[0]]
                        val newContent = target.constructContent(record.type, record.content)
                        if (newContent == record.content) return@f
                        logger.info("Updating record ${record.id} (${record.name}) in zone ${zone.id} to $newContent")
                        if (record.type == "SRV") {
                            CFPatchSRVDNSRecord(zone.id, record.id, record.ttl, target.ip, target.port).call()
                        } else {
                            CFPatchDNSRecord(zone.id, record.id, newContent, record.ttl).call()
                        }
                    }
                }
            }
            sleepForTick()
        }
    }

    private fun sleepForTick() {
        val time = getSleepableTime()
        if (time > 0) Thread.sleep(time)
        if (time < -1000) {
            val overtime = (HealthCheckerConfig.checkInterval * 1000) - time
            logger.warn("Something's taking too long! Took ${overtime}ms to finish a tick")
        }
        lastTick = System.currentTimeMillis()
    }

    private fun getSleepableTime() = (HealthCheckerConfig.checkInterval * 1000) - (System.currentTimeMillis() - lastTick)
}
