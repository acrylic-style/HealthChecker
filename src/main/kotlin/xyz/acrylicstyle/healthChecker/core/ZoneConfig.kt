package xyz.acrylicstyle.healthChecker.core

import util.Collection
import util.yaml.YamlObject
import xyz.acrylicstyle.healthChecker.util.AddressPair

data class ZoneConfig(
    val id: String,
    val groups: Collection<String, ZoneGroupConfig>,
) {
    companion object {
        fun parse(json: YamlObject): ZoneConfig {
            val id = json.getString("id") ?: throw NullPointerException("'id' in zone is not defined")
            val groupArray = json.getArray("groups") ?: throw NullPointerException("'groups' in zone is not defined")
            val groups = Collection<String, ZoneGroupConfig>()
            groupArray.forEach { o ->
                @Suppress("UNCHECKED_CAST")
                val obj = YamlObject(o as Map<String, Any>)
                val targets = obj.getArray("targets").map { any -> AddressPair.parse(any as String) }
                groups.add(obj.getString("name"), ZoneGroupConfig(obj.getString("name"), targets))
            }
            return ZoneConfig(id, groups)
        }
    }
}
