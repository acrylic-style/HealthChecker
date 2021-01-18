package xyz.acrylicstyle.healthChecker.main

import org.apache.logging.log4j.LogManager
import org.yaml.snakeyaml.Yaml
import util.Collection
import util.function.BuiltinStringConverter
import util.option.OptionParser
import util.yaml.YamlObject
import xyz.acrylicstyle.healthChecker.HealthChecker
import xyz.acrylicstyle.healthChecker.LoggedPrintStream
import xyz.acrylicstyle.healthChecker.core.HealthCheckerConfig
import xyz.acrylicstyle.healthChecker.core.ReportedException
import xyz.acrylicstyle.healthChecker.core.ZoneConfig
import xyz.acrylicstyle.healthChecker.util.Util
import xyz.acrylicstyle.healthChecker.util.cloudflare.CFDNSRecord
import xyz.acrylicstyle.healthChecker.util.cloudflare.dns.CFListDNSRecords
import xyz.acrylicstyle.healthChecker.util.cloudflare.dns.CFPatchDNSRecord
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

@Suppress("UNCHECKED_CAST")
object Main {
    private val logger = LogManager.getLogger(HealthChecker::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        System.setOut(LoggedPrintStream(System.out))
        if (!File("./config.yml").exists()) {
            logger.info("Copying default config.yml")
            Files.copy(Main::class.java.classLoader.getResourceAsStream("./config.yml")!!, File("./config.yml").toPath())
        }
        val defaultCheckInterval = HealthChecker.config.getInt("checkInterval", HealthCheckerConfig.checkInterval)
        val defaultEndpoint = HealthChecker.config.getString("endpoint", HealthCheckerConfig.endpoint)
        val defaultApiToken = HealthChecker.config.getString("apiToken")
        val parser = OptionParser()
        val spec1 = parser.accepts("checkInterval").withRequiredArg().ofType(BuiltinStringConverter.INTEGER).defaultsTo(defaultCheckInterval)
        val spec2 = parser.accepts("endpoint").withRequiredArg().defaultsTo(defaultEndpoint)
        val spec3 = parser.accepts("apiToken").withRequiredArg().defaultsTo(defaultApiToken)
        val result = parser.parse(*args)
        logger.info("HealthChecker v${Yaml().load<String>(Main::class.java.classLoader.getResourceAsStream("./version.yml"))} by acrylic-style")
        if (result.has("help")) {
            logger.info("Options:")
            logger.info("    --help")
            logger.info("        Show the help menu")
            logger.info("    --checkInterval=[$defaultCheckInterval]")
            logger.info("        Sets the interval of checking hosts")
            logger.info("    --endpoint=[$defaultEndpoint]")
            logger.info("        Sets the Cloudflare API endpoint")
            logger.info("    --apiToken=[your api token]")
            logger.info("        API Token generated from the User Profile 'API Tokens' page")
            return
        }
        HealthCheckerConfig.checkInterval = result.value(spec1)!!
        HealthCheckerConfig.endpoint = result.value(spec2)!!
        HealthCheckerConfig.apiToken = result.value(spec3)
        if (HealthCheckerConfig.apiToken == null || HealthCheckerConfig.apiToken!!.isEmpty()) {
            logger.error("API Token was not specified. Please specify in the either option or the config.yml.")
            return
        }
        val zones = HealthChecker.config.getArray("zones")
        if (zones == null || zones.isEmpty()) {
            logger.error("Zone list is empty. Please define zones and try again.")
            return
        }
        zones.forEach {
            if (it == null) return@forEach
            val obj = YamlObject(it as Map<String, Any>)
            HealthCheckerConfig.zones.add(ZoneConfig.parse(obj))
        }
        logger.info("Linking DNS Records")
        Util.allowMethods("PATCH")
        HealthCheckerConfig.zones.forEach { zone ->
            val map = Collection<String, CFDNSRecord>()
            CFListDNSRecords(zone.id).execute().forEach { dns ->
                zone.groups.forEach { name, _ ->
                    if (dns.name == name) {
                        if (dns.ttl != 60) {
                            logger.info("Setting TTL to 60 seconds for record ${dns.id} (${dns.name}) in zone ${zone.id}")
                            CFPatchDNSRecord(zone.id, dns.id, dns.content, 60).call().response
                        }
                        map.add(name, dns)
                    }
                }
            }
            HealthCheckerConfig.linkedDNSRecordList.add(zone, map)
            logger.info("Linked ${map.size} ${if (map.size == 1) "entry" else "entries"} in ${zone.id}")
        }
        try {
            HealthChecker.run()
        } catch (e: ReportedException) {
            logger.fatal("Reported exception thrown!", e)
            exitProcess(1)
        } catch (e: Throwable) {
            logger.fatal("Unreported exception thrown!", e)
            exitProcess(1)
        }
    }
}