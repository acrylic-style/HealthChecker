package xyz.acrylicstyle.healthChecker

import org.apache.logging.log4j.LogManager
import java.io.OutputStream
import java.io.PrintStream

class LoggedPrintStream(outputStream: OutputStream) : PrintStream(outputStream) {
    override fun println(debug1: String?) {
        logLine(debug1)
    }

    override fun println(debug1: Any) {
        logLine(debug1.toString())
    }

    private fun logLine(debug1: String?) {
        LOGGER.info(debug1)
    }

    companion object {
        private val LOGGER = LogManager.getLogger()
    }
}
