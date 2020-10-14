package xyz.acrylicstyle.healthChecker.core

import util.CollectionList
import xyz.acrylicstyle.healthChecker.util.AddressPair

data class ZoneGroupConfig(
    val name: String,
    val targets: CollectionList<AddressPair>,
)
