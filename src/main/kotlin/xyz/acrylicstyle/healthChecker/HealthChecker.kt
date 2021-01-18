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
import java.util.AbstractMap

object HealthChecker: Runnable {
    private val logger = LogManager.getLogger(HealthChecker::class.java)

    private var lastTick = System.currentTimeMillis()

    val config = YamlConfiguration("./config.yml").asObject()

    private fun isReachable(host: String, port: Int): AbstractMap.SimpleEntry<Boolean, String> {
        return try {
            val socket = Socket()
            val addr = InetSocketAddress(host, port)
            socket.connect(addr, 3000)
            val conn = socket.isConnected
            socket.close()
            AbstractMap.SimpleEntry(conn, addr.address.hostAddress)
        } catch (e: Exception) {
            AbstractMap.SimpleEntry(false, null)
        }
    }

    override fun run() {
        logger.info("1 tick = ${HealthCheckerConfig.checkInterval * 1000}ms")
        while (true) {
            HealthCheckerConfig.linkedDNSRecordList.forEach { zone, map ->
                map.forEach f@ { name, record ->
                    val results = CollectionList<AbstractMap.SimpleEntry<Int, String>>()
                    val promises = CollectionList<Promise<*>>()
                    zone.groups[name]!!.targets.foreach { target, i ->
                        promises.add(Promise.async {
                            try {
                                val pair = isReachable(target.ip, target.port)
                                if (pair.key) {
                                    results.add(AbstractMap.SimpleEntry(i, pair.value))
                                }
                            } catch (e: IllegalArgumentException) {
                                throw ReportedException("Illegal port or IP (${target.ip}:${target.port})", e)
                            }
                        })
                    }
                    Promise.all(*promises.toTypedArray()).complete()
                    results.sortedWith(java.util.Map.Entry.comparingByKey())
                    if (results.isNotEmpty()) {
                        val pair = results[0]
                        val target = zone.groups[name]!!.targets[pair.key]
                        val newContent = target.constructContent(pair.value, record.type, record.content)
                        if (newContent == record.content) return@f
                        logger.info("Updating record ${record.id} (${record.name}) in zone ${zone.id} to $newContent (${target.ip})")
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
