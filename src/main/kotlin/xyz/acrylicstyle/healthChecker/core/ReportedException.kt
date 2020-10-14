package xyz.acrylicstyle.healthChecker.core

class ReportedException(message: String, cause: Throwable?): RuntimeException(message, cause) {
    constructor(message: String): this(message, null)
}
