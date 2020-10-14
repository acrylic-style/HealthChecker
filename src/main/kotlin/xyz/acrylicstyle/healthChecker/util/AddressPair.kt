package xyz.acrylicstyle.healthChecker.util

data class AddressPair(val ip: String, val port: Int) {
    companion object {
        fun parse(s: String): AddressPair {
            val arr = s.split(":")
            return AddressPair(arr[0], arr[1].toInt())
        }
    }

    fun constructContent(type: String, oldContent: String): String {
        if (type == "SRV") {
            val weight = oldContent.split("\t")[0]
            return "$weight\t$port\t$ip"
        }
        return ip
    }
}
